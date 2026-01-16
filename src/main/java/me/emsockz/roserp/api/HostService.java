package me.emsockz.roserp.api;

import me.emsockz.roserp.api.HostDiagnostics;

import java.util.Optional;

/**
 * Public interface for the built-in simple HTTP host used by the plugin.
 * Other plugins should depend only on this interface.
 */
public interface HostService {
    /**
     * Start the host. Idempotent.
     * @throws IllegalStateException when start is not possible (e.g. port in use)
     */
    void start();

    /**
     * Stop the host gracefully. Idempotent.
     */
    void stop();

    /**
     * Returns true if host is running.
     */
    boolean isRunning();

    /**
     * Get URL for a pack name if available.
     * @param packName pack id
     * @return Optional with URL (HTTP) if host is running and pack exists.
     */
    Optional<String> getPackUrl(String packName);

    /**
     * Diagnostics snapshot.
     */
    HostDiagnostics getDiagnostics();
}
