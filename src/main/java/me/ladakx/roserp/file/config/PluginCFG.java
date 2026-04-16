package me.ladakx.roserp.file.config;

import me.ladakx.roserp.RoseRP;
import me.ladakx.roserp.util.ServerIPFetcher;
import me.ladakx.roserp.util.StringUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PluginCFG {

    public final String LANG;
    public final Boolean CHECK_UPDATE;
    public final String IP;
    public final Integer PORT;
    public final String HOST_URL;
    public final String HOST_BIND_ADDRESS;
    public final int HOST_BIND_PORT;
    public final String HOST_PUBLIC_SCHEME;
    public final String HOST_PUBLIC_HOST;
    public final int HOST_PUBLIC_PORT;
    public final List<String> IGNORE_FILES;
    public final List<String> JOIN_PACKS;
    public final boolean resetPackOnLeave;
    public final int HOST_READ_TIMEOUT_MS;
    public final int HOST_MAX_REQUEST_LINE_LENGTH;
    public final int HOST_MAX_ACTIVE_CONNECTIONS;
    public final boolean HOST_LOG_NOT_FOUND_REQUESTS;
    public final boolean HOST_LOG_CLIENT_DISCONNECTS;
    public final boolean HOST_ALLOW_ONLY_ONLINE_PLAYER_IPS;
    public final boolean HOST_REQUIRE_TOKENS;
    public final int HOST_TOKEN_TTL_SECONDS;
    public final boolean HOST_API_ENABLED;
    public final String HOST_API_PATH;
    public final List<String> HOST_API_ALLOWED_IPS;

    public PluginCFG() {
        FileConfiguration cfg = RoseRP.getInstance().getConfig();
        LANG = cfg.getString("lang");
        CHECK_UPDATE = cfg.getBoolean("checkUpdate", true);
        int legacyPort = cfg.getInt("port");
        String legacyIp = cfg.contains("ip") ? cfg.getString("ip") : ServerIPFetcher.getPublicIP();

        HOST_BIND_ADDRESS = normalizeBindAddress(cfg.getString("host.bind.address", ""));
        HOST_BIND_PORT = normalizePort(cfg.getInt("host.bind.port", legacyPort > 0 ? legacyPort : 8085), 8085);
        HOST_PUBLIC_SCHEME = normalizeScheme(cfg.getString("host.public.scheme", "http"));
        HOST_PUBLIC_HOST = normalizePublicHost(cfg.getString("host.public.host", legacyIp));
        HOST_PUBLIC_PORT = normalizePort(cfg.getInt("host.public.port", HOST_BIND_PORT), HOST_BIND_PORT);

        IP = HOST_PUBLIC_HOST;
        PORT = HOST_BIND_PORT;
        HOST_URL = buildHostUrl(HOST_PUBLIC_SCHEME, HOST_PUBLIC_HOST, HOST_PUBLIC_PORT);
        IGNORE_FILES = cfg.getStringList("ignoreFiles");
        JOIN_PACKS = cfg.getStringList("joinPacks");
        resetPackOnLeave = cfg.getBoolean("resetPackOnLeave");
        HOST_READ_TIMEOUT_MS = Math.max(1_000, cfg.getInt("host.readTimeoutMs", 15_000));
        HOST_MAX_REQUEST_LINE_LENGTH = Math.max(256, cfg.getInt("host.maxRequestLineLength", 2_048));
        HOST_MAX_ACTIVE_CONNECTIONS = Math.max(8, cfg.getInt("host.maxActiveConnections", 128));
        HOST_LOG_NOT_FOUND_REQUESTS = cfg.getBoolean("host.logNotFoundRequests", false);
        HOST_LOG_CLIENT_DISCONNECTS = cfg.getBoolean("host.logClientDisconnects", false);
        HOST_ALLOW_ONLY_ONLINE_PLAYER_IPS = cfg.getBoolean("host.allowOnlyOnlinePlayerIps", false);
        HOST_REQUIRE_TOKENS = cfg.getBoolean("host.requireTokens", false);
        HOST_TOKEN_TTL_SECONDS = Math.max(10, cfg.getInt("host.tokenTtlSeconds", 300));
        HOST_API_ENABLED = cfg.getBoolean("host.api.enabled", false);
        HOST_API_PATH = normalizeApiPath(cfg.getString("host.api.path", "/api/pack-link"));
        HOST_API_ALLOWED_IPS = cfg.getStringList("host.api.allowedIps");
    }

    private String normalizeApiPath(String value) {
        if (StringUtil.isBlank(value)) {
            return "/api/pack-link";
        }

        return value.startsWith("/") ? value : "/" + value;
    }

    private int normalizePort(int value, int fallback) {
        return value > 0 && value <= 65535 ? value : fallback;
    }

    private String normalizeBindAddress(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String normalizePublicHost(String value) {
        if (StringUtil.isBlank(value)) {
            return ServerIPFetcher.getPublicIP();
        }

        return value.trim();
    }

    private String normalizeScheme(String value) {
        if (StringUtil.isBlank(value)) {
            return "http";
        }

        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String buildHostUrl(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port;
    }
}
