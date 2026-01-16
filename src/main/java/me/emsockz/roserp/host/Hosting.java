package me.emsockz.roserp.host;

import me.emsockz.roserp.RoseRP;
import me.emsockz.roserp.RoseRPLogger;
import me.emsockz.roserp.api.HostService;
import me.emsockz.roserp.api.HostDiagnostics;
import me.emsockz.roserp.file.config.MessagesCFG;
import me.emsockz.roserp.pack.Pack;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Robust host implementation for serving resourcepack .zip files from plugin folder.
 *
 * - Single acceptor thread
 * - Fixed worker pool for connections
 * - Safe path resolution (only files under dataFolder/resourcepacks/<name>/<name>.zip)
 * - Simple diagnostics counters
 */
public class Hosting implements HostService {

    private final JavaPlugin plugin;
    private final int port;
    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    private final ExecutorService workerPool;
    private final Thread acceptorThread;

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalServed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    private volatile Instant startedAt = null;
    private volatile Instant lastErrorAt = null;

    private volatile String lastErrorMessage = null;

    private volatile Instant lastAcceptAt;
    private volatile Instant lastServeAt;

    // configuration
    private final int socketAcceptTimeoutMs = 2000; // short accept timeout to allow fast shutdown

    public Hosting(JavaPlugin plugin, int port, int workerPoolSize) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.port = port;
        this.workerPool = Executors.newFixedThreadPool(Math.max(2, workerPoolSize),
                r -> {
                    Thread t = new Thread(r, "roserp-host-worker");
                    t.setDaemon(true);
                    return t;
                });

        this.acceptorThread = new Thread(this::acceptLoop, "roserp-host-acceptor");
    }

    @Override
    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.setSoTimeout(socketAcceptTimeoutMs);
            running = true;
            startedAt = Instant.now();
            acceptorThread.start();

            lastAcceptAt = null;
            lastServeAt = null;

            RoseRPLogger.info("Hosting started on port " + port);
            MessagesCFG.HOST_STARTED.sendMessage(Bukkit.getConsoleSender(), "{ip}", RoseRP.getPluginConfig().IP, "{port}", String.valueOf(port));
        } catch (Exception ex) {
            running = false;
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

        if (acceptorThread != null) {
            acceptorThread.interrupt();
        }

        if (workerPool != null) {
            workerPool.shutdownNow();
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                RoseRPLogger.error("Failed to close server socket", e);
            }
        }

        RoseRPLogger.info("Resourcepack host stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Optional<String> getPackUrl(String packName) {
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

        String ip = RoseRP.getPluginConfig().IP;
        if (ip == null || ip.isBlank()) {
            RoseRPLogger.error("PluginCFG.IP is null or empty, cannot build pack URL");
            return Optional.empty();
        }

        int port = RoseRP.getPluginConfig().PORT;
        if (port <= 0 || port > 65535) {
            RoseRPLogger.error("Invalid port in config: " + port);
            return Optional.empty();
        }

        String url = String.format(java.util.Locale.ROOT,
                "http://%s:%d/%s.zip", ip, port, packName);

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
                }
                socket.setSoTimeout(15_000); // per-connection read timeout
                workerPool.submit(() -> handleSocket(socket));
            } catch (Throwable t) {
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
                sendSimpleResponse(out, "400 Bad Request", "text/plain", "Invalid request");
                MessagesCFG.HOST_INVALID_REQUEST.sendMessage(Bukkit.getConsoleSender());
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendSimpleResponse(out, "400 Bad Request", "text/plain", "Invalid request");
                return;
            }

            String method = parts[0];
            String path = parts[1];

            if (!"GET".equalsIgnoreCase(method)) {
                sendSimpleResponse(out, "405 Method Not Allowed", "text/plain", "Only GET is supported");
                return;
            }

            // Expecting /<pack>.zip or /<pack>
            String packName = parsePackName(path);
            if (packName == null) {
                sendSimpleResponse(out, "400 Bad Request", "text/plain", "Invalid pack name");
                return;
            }

            Pack pack = RoseRP.getPack(packName);
            if (pack == null) {
                sendSimpleResponse(out, "404 Not Found", "text/plain", "Not found");
                MessagesCFG.HOST_FILE_NOT_FOUND.sendMessage(Bukkit.getConsoleSender(), "{pack}", packName);
                return;
            }

            File zipFile = new File(RoseRP.getInstance().getDataFolder(), "resourcepacks" + File.separator + packName + File.separator + packName + ".zip");
            if (!zipFile.exists() || !zipFile.isFile()) {
                sendSimpleResponse(out, "404 Not Found", "text/plain", "Not found");
                MessagesCFG.HOST_FILE_NOT_FOUND.sendMessage(Bukkit.getConsoleSender(), "{pack}", packName);
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
            catch (java.net.SocketException e) {
                // клієнт закрив з'єднання — це не критична помилка
                totalErrors.incrementAndGet();
                lastErrorAt = Instant.now();
                lastErrorMessage = "Client disconnected while streaming: " + socket.getRemoteSocketAddress();
                //RoseRPLogger.warn("Client disconnected while streaming pack " + packName +
                //        " from " + socket.getRemoteSocketAddress());
            }
            catch (IOException e) {
                totalErrors.incrementAndGet();
                lastErrorAt = Instant.now();
                lastErrorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                RoseRPLogger.error("Error streaming pack " + packName, e);
            }
        } catch (java.net.SocketTimeoutException e) {
            totalErrors.incrementAndGet();
            lastErrorAt = Instant.now();
            lastErrorMessage = "Read timed out from client " + socket.getRemoteSocketAddress();
            //RoseRPLogger.warn("Connection handling error: Read timed out from " + socket.getRemoteSocketAddress());
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
        boolean seenCR = false;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                seenCR = true;
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
            if (sb.length() > 2048) break;
        }
        return sb.length() == 0 ? null : sb.toString().trim();
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
        if (!acceptorThread.isAlive()) return false;
        if (workerPool.isShutdown() || workerPool.isTerminated()) return false;

//        Instant now = Instant.now();
//        if (lastAcceptAt != null && now.minusSeconds(120).isAfter(lastAcceptAt)) {
//            RoseRPLogger.warn("Host has not accepted connections for 120s");
//        }
//        if (lastServeAt != null && now.minusSeconds(300).isAfter(lastServeAt)) {
//            RoseRPLogger.warn("Host has not served any pack for 300s");
//        }
        return true;
    }
}
