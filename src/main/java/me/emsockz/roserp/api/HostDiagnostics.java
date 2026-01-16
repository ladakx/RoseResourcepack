package me.emsockz.roserp.api;

import java.time.Instant;

/**
 * Lightweight diagnostics snapshot returned by HostService.getDiagnostics()
 */
public final class HostDiagnostics {

    private final boolean running;
    private final int activeConnections;
    private final long totalServed;
    private final long totalErrors;
    private final Instant startedAt;
    private final Instant lastAcceptAt;
    private final Instant lastServeAt;
    private final Instant lastErrorAt;
    private final String lastError;

    public HostDiagnostics(
            boolean running,
            int activeConnections,
            long totalServed,
            long totalErrors,
            Instant startedAt,
            Instant lastAcceptAt,
            Instant lastServeAt,
            Instant lastErrorAt,
            String lastError
    ) {
        this.running = running;
        this.activeConnections = activeConnections;
        this.totalServed = totalServed;
        this.totalErrors = totalErrors;
        this.startedAt = startedAt;
        this.lastAcceptAt = lastAcceptAt;
        this.lastServeAt = lastServeAt;
        this.lastErrorAt = lastErrorAt;
        this.lastError = lastError;
    }

    public boolean isRunning() { return running; }
    public int getActiveConnections() { return activeConnections; }
    public long getTotalServed() { return totalServed; }
    public long getTotalErrors() { return totalErrors; }

    public Instant getStartedAt() { return startedAt; }
    public Instant getLastAcceptAt() { return lastAcceptAt; }
    public Instant getLastServeAt() { return lastServeAt; }
    public Instant getLastErrorAt() { return lastErrorAt; }
    public String getLastError() { return lastError; }
}
