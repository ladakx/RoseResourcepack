package me.ladakx.roserp.host;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.RoseRPLogger;
import me.ladakx.roserp.api.HostService;
import me.ladakx.roserp.api.HostDiagnostics;
import me.ladakx.roserp.file.config.MessagesCFG;
import me.ladakx.roserp.pack.Pack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.security.SecureRandom;

/**
 * Robust host implementation for serving resourcepack .zip files from plugin folder.
 *
 * - Single acceptor thread
 * - Fixed worker pool for connections
 * - Safe path resolution (only files under dataFolder/resourcepacks/<name>/<name>.zip)
 * - Simple diagnostics counters
 */
public class Hosting implements HostService {

    private final String bindAddress;
    private final int bindPort;
    private final int workerPoolSize;
    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    private volatile ExecutorService workerPool;
    private volatile Thread acceptorThread;

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalServed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong suppressedNotFoundRequests = new AtomicLong(0);
    private final AtomicLong suppressedDisconnects = new AtomicLong(0);

    private volatile Instant startedAt = null;
    private volatile Instant lastErrorAt = null;

    private volatile String lastErrorMessage = null;

    private volatile Instant lastAcceptAt;
    private volatile Instant lastServeAt;

    // configuration
    private final int socketAcceptTimeoutMs = 2000; // short accept timeout to allow fast shutdown
    private final int socketReadTimeoutMs;
    private final int maxRequestLineLength;
    private final int maxActiveConnections;
    private final boolean logNotFoundRequests;
    private final boolean logClientDisconnects;
    private final boolean allowOnlyOnlinePlayerIps;
    private final boolean requireTokens;
    private final int tokenTtlSeconds;
    private final boolean apiEnabled;
    private final String apiPath;
    private final List<String> apiAllowedIps;
    private final String publicScheme;
    private final String publicHost;
    private final int publicPort;
    private final boolean allowOnlyOnlinePlayerIpsEffective;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, TokenEntry> activeTokens = new ConcurrentHashMap<>();

    public Hosting(org.bukkit.plugin.java.JavaPlugin plugin, int workerPoolSize) {
        Objects.requireNonNull(plugin, "plugin");
        this.bindAddress = RoseRP.getPluginConfig().HOST_BIND_ADDRESS;
        this.bindPort = RoseRP.getPluginConfig().HOST_BIND_PORT;
        this.workerPoolSize = Math.max(2, workerPoolSize);
        this.socketReadTimeoutMs = RoseRP.getPluginConfig().HOST_READ_TIMEOUT_MS;
        this.maxRequestLineLength = RoseRP.getPluginConfig().HOST_MAX_REQUEST_LINE_LENGTH;
        this.maxActiveConnections = RoseRP.getPluginConfig().HOST_MAX_ACTIVE_CONNECTIONS;
        this.logNotFoundRequests = RoseRP.getPluginConfig().HOST_LOG_NOT_FOUND_REQUESTS;
        this.logClientDisconnects = RoseRP.getPluginConfig().HOST_LOG_CLIENT_DISCONNECTS;
        this.allowOnlyOnlinePlayerIps = RoseRP.getPluginConfig().HOST_ALLOW_ONLY_ONLINE_PLAYER_IPS;
        this.requireTokens = RoseRP.getPluginConfig().HOST_REQUIRE_TOKENS;
        this.tokenTtlSeconds = RoseRP.getPluginConfig().HOST_TOKEN_TTL_SECONDS;
        this.apiEnabled = RoseRP.getPluginConfig().HOST_API_ENABLED;
        this.apiPath = RoseRP.getPluginConfig().HOST_API_PATH;
        this.apiAllowedIps = List.copyOf(RoseRP.getPluginConfig().HOST_API_ALLOWED_IPS);
        this.publicScheme = RoseRP.getPluginConfig().HOST_PUBLIC_SCHEME;
        this.publicHost = RoseRP.getPluginConfig().HOST_PUBLIC_HOST;
        this.publicPort = RoseRP.getPluginConfig().HOST_PUBLIC_PORT;
        this.allowOnlyOnlinePlayerIpsEffective = allowOnlyOnlinePlayerIps && usesSameAddressForBindAndPublicHost();
        this.workerPool = null;
        this.acceptorThread = null;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            workerPool = Executors.newFixedThreadPool(workerPoolSize,
                    r -> {
                        Thread t = new Thread(r, "roserp-host-worker");
                        t.setDaemon(true);
                        return t;
                    });

            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            if (bindAddress == null || bindAddress.isBlank()) {
                serverSocket.bind(new InetSocketAddress(bindPort));
            } else {
                serverSocket.bind(new InetSocketAddress(bindAddress, bindPort));
            }
            serverSocket.setSoTimeout(socketAcceptTimeoutMs);
            acceptorThread = new Thread(this::acceptLoop, "roserp-host-acceptor");
            acceptorThread.setDaemon(true);
            running = true;
            startedAt = Instant.now();
            acceptorThread.start();

            lastAcceptAt = null;
            lastServeAt = null;

            if (allowOnlyOnlinePlayerIps && !allowOnlyOnlinePlayerIpsEffective) {
                RoseRPLogger.warn("host.allowOnlyOnlinePlayerIps is disabled because host.bind.address and host.public.host differ");
            }

            RoseRPLogger.info("Hosting started on " + describeBindAddress());
            MessagesCFG.HOST_STARTED.sendMessage(
                    Bukkit.getConsoleSender(),
                    "{ip}",
                    bindAddress == null || bindAddress.isBlank() ? "0.0.0.0" : bindAddress,
                    "{port}",
                    String.valueOf(bindPort)
            );
        } catch (Exception ex) {
            running = false;
            shutdownWorkerPool();
            lastErrorAt = Instant.now();
            lastErrorMessage = ex.getMessage();
            RoseRPLogger.error("Failed to start Hosting: " + ex.getMessage(), ex);
            MessagesCFG.HOST_ERROR.sendMessage(Bukkit.getConsoleSender(), "{error}", ex.getMessage());
            throw new IllegalStateException("Failed to start host", ex);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        Thread currentAcceptor = acceptorThread;
        if (currentAcceptor != null) {
            acceptorThread.interrupt();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                RoseRPLogger.error("Failed to close server socket", e);
            }
        }

        shutdownWorkerPool();
        acceptorThread = null;
        serverSocket = null;

        RoseRPLogger.info("Resourcepack host stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Optional<String> getPackUrl(String packName) {
        return getPackUrl(packName, null);
    }

    @Override
    public Optional<String> getPackUrl(String packName, String clientIp) {
        if (!running) {
            RoseRPLogger.warn("getPackUrl called while host is not running");
            return Optional.empty();
        }

        if (packName == null || packName.isBlank()) {
            RoseRPLogger.warn("getPackUrl called with empty packName");
            return Optional.empty();
        }

        if (!RoseRP.hasPack(packName)) {
            RoseRPLogger.warn("getPackUrl called for unknown pack: " + packName);
            return Optional.empty();
        }

        if (publicHost == null || publicHost.isBlank()) {
            RoseRPLogger.error("Public host is null or empty, cannot build pack URL");
            return Optional.empty();
        }

        if (publicPort <= 0 || publicPort > 65535) {
            RoseRPLogger.error("Invalid public port in config: " + publicPort);
            return Optional.empty();
        }

        String url = String.format(java.util.Locale.ROOT, "%s://%s:%d/%s.zip", publicScheme, publicHost, publicPort, packName);

        if (requireTokens) {
            String token = issueToken(packName, clientIp);
            url = url + "?token=" + token;
        }

        return Optional.of(url);
    }

    @Override
    public HostDiagnostics getDiagnostics() {
        return new HostDiagnostics(
                running,
                activeConnections.get(),
                totalServed.get(),
                totalErrors.get(),
                startedAt,
                lastAcceptAt,
                lastServeAt,
                lastErrorAt,
                lastErrorMessage
        );
    }


    private void acceptLoop() {
        while (running) {
            try {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                    lastAcceptAt = Instant.now();
                } catch (SocketTimeoutException ste) {
                    // periodic wakeup to check running flag
                    continue;
                } catch (SocketException se) {
                    if (!running || serverSocket == null || serverSocket.isClosed()) {
                        break;
                    }
                    throw se;
                }
                socket.setSoTimeout(socketReadTimeoutMs);
                if (activeConnections.get() >= maxActiveConnections) {
                    closeQuietly(socket);
                    continue;
                }
                ExecutorService currentWorkerPool = workerPool;
                if (currentWorkerPool == null || currentWorkerPool.isShutdown()) {
                    closeQuietly(socket);
                    continue;
                }
                currentWorkerPool.submit(() -> handleSocket(socket));
            } catch (Throwable t) {
                if (!running) {
                    break;
                }
                totalErrors.incrementAndGet();
                lastErrorAt = Instant.now();
                lastErrorMessage = t.getMessage();
                RoseRPLogger.error("Error in accept loop", t);
            }
        }
    }

    private void handleSocket(Socket socket) {
        activeConnections.incrementAndGet();
        try (Socket s = socket;

             BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
             OutputStream out = new BufferedOutputStream(s.getOutputStream())) {

            // Read request line (very small lightweight parser)
            String requestLine = readLine(bis);
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendSimpleResponse(out, "400 Bad Request", "text/plain", "Invalid request");
                return;
            }

            String method = parts[0];
            String rawTarget = parts[1];
            discardHeaders(bis);

            RequestTarget requestTarget = parseRequestTarget(rawTarget);
            if (requestTarget == null) {
                sendSimpleResponse(out, "400 Bad Request", "text/plain", "Invalid request");
                return;
            }

            if (apiEnabled && apiPath.equals(requestTarget.path())) {
                handleApiRequest(socket, out, method, requestTarget);
                return;
            }

            boolean headRequest = "HEAD".equalsIgnoreCase(method);
            if (!"GET".equalsIgnoreCase(method) && !headRequest) {
                sendSimpleResponse(out, "405 Method Not Allowed", "text/plain", "Only GET and HEAD are supported");
                return;
            }

            // Expecting /<pack>.zip or /<pack>
            String packName = parsePackName(requestTarget.path());
            if (packName == null) {
                sendSimpleResponse(out, "404 Not Found", "text/plain", "Not found");
                return;
            }

            TokenValidation tokenValidation = validateToken(requestTarget.token(), packName, socket.getInetAddress());
            if (requireTokens && !tokenValidation.valid()) {
                sendSimpleResponse(out, "403 Forbidden", "text/plain", "Forbidden");
                return;
            }

            if (allowOnlyOnlinePlayerIpsEffective && !tokenValidation.bypassOnlinePlayerCheck() && !isAllowedRemoteAddress(socket)) {
                sendSimpleResponse(out, "403 Forbidden", "text/plain", "Forbidden");
                return;
            }

            Pack pack = RoseRP.getPack(packName);
            if (pack == null) {
                sendSimpleResponse(out, "404 Not Found", "text/plain", "Not found");
                logNotFound(packName);
                return;
            }

            File zipFile = new File(RoseRP.getInstance().getDataFolder(), "resourcepacks" + File.separator + packName + File.separator + packName + ".zip");
            if (!zipFile.exists() || !zipFile.isFile()) {
                sendSimpleResponse(out, "404 Not Found", "text/plain", "Not found");
                logNotFound(packName);
                return;
            }

            // Serve file with content length
            long length = zipFile.length();
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/zip\r\n" +
                    "Content-Length: " + length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            out.write(headers.getBytes(StandardCharsets.UTF_8));
            out.flush();

            if (headRequest) {
                totalServed.incrementAndGet();
                lastServeAt = Instant.now();
                return;
            }

            try (InputStream fin = Files.newInputStream(zipFile.toPath())) {
                byte[] buf = new byte[64 * 1024];
                int r;
                while ((r = fin.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
                out.flush();
                totalServed.incrementAndGet();
                lastServeAt = Instant.now();
            }
            catch (SocketException e) {
                handleClientDisconnect(socket, e, "streaming pack " + packName);
            }
            catch (IOException e) {
                totalErrors.incrementAndGet();
                lastErrorAt = Instant.now();
                lastErrorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                RoseRPLogger.error("Error streaming pack " + packName, e);
            }
        } catch (SocketTimeoutException e) {
            handleClientDisconnect(socket, e, "reading request");
            return;
        } catch (SocketException e) {
            handleClientDisconnect(socket, e, "handling socket");
            return;
        } catch (IOException e) {
            totalErrors.incrementAndGet();
            lastErrorAt = Instant.now();
            lastErrorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            RoseRPLogger.error("Connection handling error", e);
            return;
        } finally {
            activeConnections.decrementAndGet();
        }
    }

    // Read single line from stream terminated by CRLF or LF
    private String readLine(BufferedInputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                int next = in.read();
                if (next == '\n') break;
                if (next == -1) break;
                sb.append((char) next);
                continue;
            } else if (c == '\n') {
                break;
            } else {
                sb.append((char) c);
            }
            // small safeguard to avoid pathological long headers
            if (sb.length() > maxRequestLineLength) break;
        }
        return sb.length() == 0 ? null : sb.toString().trim();
    }

    private void discardHeaders(BufferedInputStream in) throws IOException {
        int headerLines = 0;
        while (headerLines < 64) {
            String headerLine = readLine(in);
            if (headerLine == null || headerLine.isEmpty()) {
                return;
            }
            headerLines++;
        }
        throw new IOException("Too many request headers");
    }

    private void sendSimpleResponse(OutputStream out, String status, String contentType, String body) {
        try {
            String response = "HTTP/1.1 " + status + "\r\n" +
                    "Content-Type: " + contentType + "; charset=utf-8\r\n" +
                    "Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    body;
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (SocketException e) {
            suppressedDisconnects.incrementAndGet();
            if (logClientDisconnects) {
                RoseRPLogger.warn("Failed to send response: " + e.getMessage());
            }
        } catch (IOException e) {
            RoseRPLogger.warn("Failed to send response: " + e.getMessage());
        }
    }

    // Accepts "/pack.zip", "/pack", "/pack/"
    private String parsePackName(String path) {
        if (path == null) return null;
        if (path.startsWith("/")) path = path.substring(1);
        if (path.endsWith("/")) path = path.substring(0, path.length()-1);
        if (path.endsWith(".zip")) path = path.substring(0, path.length()-4);
        if (path.isBlank()) return null;
        // Basic sanitization — disallow path separators
        if (path.contains("..") || path.contains("/") || path.contains("\\"))
            return null;
        return path;
    }

    public boolean isReallyAlive() {
        if (!running) return false;
        if (serverSocket == null || serverSocket.isClosed()) return false;
        if (acceptorThread == null || !acceptorThread.isAlive()) return false;
        if (workerPool == null || workerPool.isShutdown() || workerPool.isTerminated()) return false;

//        Instant now = Instant.now();
//        if (lastAcceptAt != null && now.minusSeconds(120).isAfter(lastAcceptAt)) {
//            RoseRPLogger.warn("Host has not accepted connections for 120s");
//        }
//        if (lastServeAt != null && now.minusSeconds(300).isAfter(lastServeAt)) {
//            RoseRPLogger.warn("Host has not served any pack for 300s");
//        }
        return true;
    }

    private void logNotFound(String packName) {
        if (logNotFoundRequests) {
            MessagesCFG.HOST_FILE_NOT_FOUND.sendMessage(Bukkit.getConsoleSender(), "{pack}", packName);
        } else {
            suppressedNotFoundRequests.incrementAndGet();
        }
    }

    private void handleClientDisconnect(Socket socket, IOException e, String action) {
        suppressedDisconnects.incrementAndGet();
        lastErrorAt = Instant.now();
        lastErrorMessage = e.getClass().getSimpleName() + " during " + action + " from " + safeRemoteAddress(socket);

        if (logClientDisconnects) {
            totalErrors.incrementAndGet();
            RoseRPLogger.warn("Client disconnected while " + action + ": " + e.getMessage());
        }
    }

    private String safeRemoteAddress(Socket socket) {
        try {
            return String.valueOf(socket.getRemoteSocketAddress());
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void shutdownWorkerPool() {
        ExecutorService currentWorkerPool = workerPool;
        if (currentWorkerPool != null) {
            currentWorkerPool.shutdownNow();
            workerPool = null;
        }
    }

    private String issueToken(String packName, String clientIp) {
        return issueToken(packName, clientIp, false);
    }

    private String issueToken(String packName, String clientIp, boolean bypassOnlinePlayerCheck) {
        pruneExpiredTokens();

        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = Instant.now().plusSeconds(tokenTtlSeconds);
        activeTokens.put(token, new TokenEntry(packName, clientIp, expiresAt, bypassOnlinePlayerCheck));
        return token;
    }

    private TokenValidation validateToken(String token, String packName, InetAddress remoteAddress) {
        if (!requireTokens) {
            return TokenValidation.notRequired();
        }

        if (token == null || token.isBlank()) {
            return TokenValidation.invalid();
        }

        TokenEntry tokenEntry = activeTokens.get(token);
        if (tokenEntry == null) {
            return TokenValidation.invalid();
        }

        if (Instant.now().isAfter(tokenEntry.expiresAt())) {
            activeTokens.remove(token);
            return TokenValidation.invalid();
        }

        if (!tokenEntry.packName().equals(packName)) {
            return TokenValidation.invalid();
        }

        String remoteIp = remoteAddress != null ? remoteAddress.getHostAddress() : null;
        if (tokenEntry.clientIp() != null && !tokenEntry.clientIp().equals(remoteIp)) {
            return TokenValidation.invalid();
        }

        return new TokenValidation(true, tokenEntry.bypassOnlinePlayerCheck());
    }

    private void pruneExpiredTokens() {
        Instant now = Instant.now();
        for (Map.Entry<String, TokenEntry> entry : activeTokens.entrySet()) {
            if (now.isAfter(entry.getValue().expiresAt())) {
                activeTokens.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private RequestTarget parseRequestTarget(String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            return null;
        }

        String path = rawTarget;
        Map<String, String> queryParameters = Map.of();
        int queryIndex = rawTarget.indexOf('?');
        if (queryIndex >= 0) {
            path = rawTarget.substring(0, queryIndex);
            String query = rawTarget.substring(queryIndex + 1);
            queryParameters = parseQueryParameters(query);
            if (queryParameters == null) {
                return null;
            }
        }

        return new RequestTarget(path, queryParameters);
    }

    private Map<String, String> parseQueryParameters(String query) {
        Map<String, String> parameters = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return parameters;
        }

        for (String part : query.split("&")) {
            if (part.isEmpty()) {
                continue;
            }

            int separatorIndex = part.indexOf('=');
            String rawKey = separatorIndex >= 0 ? part.substring(0, separatorIndex) : part;
            String rawValue = separatorIndex >= 0 ? part.substring(separatorIndex + 1) : "";

            try {
                String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
                String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
                if (!key.isBlank()) {
                    parameters.put(key, value);
                }
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        return parameters;
    }

    private void handleApiRequest(Socket socket, OutputStream out, String method, RequestTarget requestTarget) {
        if (!"GET".equalsIgnoreCase(method)) {
            sendSimpleResponse(out, "405 Method Not Allowed", "application/json", "{\"error\":\"method_not_allowed\"}");
            return;
        }

        if (!isApiClientAllowed(socket)) {
            sendSimpleResponse(out, "403 Forbidden", "application/json", "{\"error\":\"forbidden\"}");
            return;
        }

        String packName = requestTarget.queryParameters().get("pack");
        if (packName == null || packName.isBlank()) {
            sendSimpleResponse(out, "400 Bad Request", "application/json", "{\"error\":\"missing_pack\"}");
            return;
        }

        if (!RoseRP.hasPack(packName)) {
            sendSimpleResponse(out, "404 Not Found", "application/json", "{\"error\":\"pack_not_found\"}");
            return;
        }

        String clientIp = normalizeClientIp(requestTarget.queryParameters().get("ip"));
        Optional<String> baseUrl = buildBasePackUrl(packName);
        if (baseUrl.isEmpty()) {
            sendSimpleResponse(out, "500 Internal Server Error", "application/json", "{\"error\":\"host_unavailable\"}");
            return;
        }

        String downloadUrl = baseUrl.get();
        if (requireTokens) {
            downloadUrl = downloadUrl + "?token=" + issueToken(packName, clientIp, true);
        }

        sendSimpleResponse(out, "200 OK", "application/json", "{\"url\":\"" + escapeJson(downloadUrl) + "\"}");
    }

    private Optional<String> buildBasePackUrl(String packName) {
        if (!running) {
            RoseRPLogger.warn("getPackUrl called while host is not running");
            return Optional.empty();
        }

        if (packName == null || packName.isBlank()) {
            RoseRPLogger.warn("getPackUrl called with empty packName");
            return Optional.empty();
        }

        if (!RoseRP.hasPack(packName)) {
            RoseRPLogger.warn("getPackUrl called for unknown pack: " + packName);
            return Optional.empty();
        }

        if (publicHost == null || publicHost.isBlank()) {
            RoseRPLogger.error("Public host is null or empty, cannot build pack URL");
            return Optional.empty();
        }

        if (publicPort <= 0 || publicPort > 65535) {
            RoseRPLogger.error("Invalid public port in config: " + publicPort);
            return Optional.empty();
        }

        return Optional.of(String.format(java.util.Locale.ROOT, "%s://%s:%d/%s.zip", publicScheme, publicHost, publicPort, packName));
    }

    private boolean isAllowedRemoteAddress(Socket socket) {
        InetAddress remoteAddress = socket.getInetAddress();
        if (remoteAddress == null) {
            return false;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player player : onlinePlayers) {
            InetSocketAddress playerAddress = player.getAddress();
            if (playerAddress == null || playerAddress.getAddress() == null) {
                continue;
            }

            if (remoteAddress.equals(playerAddress.getAddress())) {
                return true;
            }
        }

        return false;
    }

    private boolean isApiClientAllowed(Socket socket) {
        if (!apiEnabled || apiAllowedIps.isEmpty()) {
            return false;
        }

        InetAddress remoteAddress = socket.getInetAddress();
        if (remoteAddress == null) {
            return false;
        }

        String hostAddress = remoteAddress.getHostAddress();
        return apiAllowedIps.contains(hostAddress);
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return null;
        }

        try {
            return InetAddress.getByName(clientIp).getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private boolean usesSameAddressForBindAndPublicHost() {
        String normalizedBindAddress = normalizeAddress(bindAddress);
        String normalizedPublicHost = normalizeAddress(publicHost);

        if (normalizedBindAddress == null || normalizedPublicHost == null) {
            return false;
        }

        return normalizedBindAddress.equals(normalizedPublicHost);
    }

    private String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if ("0.0.0.0".equals(normalized) || "::".equals(normalized) || "[::]".equals(normalized)) {
            return null;
        }

        return normalized;
    }

    private String describeBindAddress() {
        return (bindAddress == null || bindAddress.isBlank() ? "0.0.0.0" : bindAddress) + ":" + bindPort;
    }

    private record TokenEntry(String packName, String clientIp, Instant expiresAt, boolean bypassOnlinePlayerCheck) {}

    private record RequestTarget(String path, Map<String, String> queryParameters) {
        private String token() {
            return queryParameters.get("token");
        }
    }

    private record TokenValidation(boolean valid, boolean bypassOnlinePlayerCheck) {
        private static TokenValidation invalid() {
            return new TokenValidation(false, false);
        }

        private static TokenValidation notRequired() {
            return new TokenValidation(true, false);
        }
    }
}
