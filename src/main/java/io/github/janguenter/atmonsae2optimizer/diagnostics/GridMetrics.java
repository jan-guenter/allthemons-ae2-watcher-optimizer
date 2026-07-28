package io.github.janguenter.atmonsae2optimizer.diagnostics;

import java.util.concurrent.atomic.AtomicLong;

public final class GridMetrics {
    private static final AtomicLong NEXT_UNRESOLVED_ID = new AtomicLong();

    private String gridId;
    private String dimension = "unresolved";
    private String scheduleIdentity;
    private long ticksObserved;
    private int watcherCount;
    private int uniqueExactInterestCount;
    private int watchAllWatcherCount;
    private boolean throttlingActive;
    private int configuredIntervalTicks = 1;
    private int phase;
    private long scheduledCensuses;
    private long cacheDrivenRebuilds;
    private long skippedCensuses;
    private long changedKeys;
    private long callbackDispatches;
    private long totalRefreshNanos;
    private long maximumRefreshNanos;
    private long lastRefreshNanos;
    private long lastLogTick;

    public GridMetrics() {
        var unresolvedId = NEXT_UNRESOLVED_ID.getAndIncrement();
        this.gridId = "unresolved-" + unresolvedId;
        this.scheduleIdentity = this.gridId;
    }

    public synchronized void identify(String resolvedGridId, String resolvedDimension) {
        if (!gridId.startsWith("unresolved-")) {
            return;
        }
        gridId = resolvedGridId;
        dimension = resolvedDimension;
        scheduleIdentity = resolvedGridId + '@' + resolvedDimension;
    }

    public synchronized String scheduleIdentity() {
        return scheduleIdentity;
    }

    public synchronized boolean observe(
            int currentWatcherCount,
            int currentUniqueExactInterestCount,
            int currentWatchAllWatcherCount,
            boolean currentThrottlingActive,
            int currentConfiguredIntervalTicks,
            int currentPhase) {
        var gainedWatchAll = watchAllWatcherCount == 0 && currentWatchAllWatcherCount > 0;
        ticksObserved++;
        watcherCount = currentWatcherCount;
        uniqueExactInterestCount = currentUniqueExactInterestCount;
        watchAllWatcherCount = currentWatchAllWatcherCount;
        throttlingActive = currentThrottlingActive;
        configuredIntervalTicks = currentConfiguredIntervalTicks;
        phase = currentPhase;
        return gainedWatchAll;
    }

    public synchronized void recordSkippedCensus() {
        skippedCensuses++;
    }

    public synchronized void recordRefresh(boolean scheduled, long elapsedNanos) {
        if (scheduled) {
            scheduledCensuses++;
        } else {
            cacheDrivenRebuilds++;
        }
        lastRefreshNanos = elapsedNanos;
        totalRefreshNanos += elapsedNanos;
        maximumRefreshNanos = Math.max(maximumRefreshNanos, elapsedNanos);
    }

    public synchronized void recordNotification(int callbackCount) {
        changedKeys++;
        callbackDispatches += callbackCount;
    }

    public synchronized boolean shouldLog(long tick, int intervalTicks) {
        if (tick - lastLogTick < intervalTicks) {
            return false;
        }
        lastLogTick = tick;
        return true;
    }

    public synchronized GridSnapshot snapshot() {
        return new GridSnapshot(
                gridId,
                dimension,
                ticksObserved,
                watcherCount,
                uniqueExactInterestCount,
                watchAllWatcherCount,
                throttlingActive,
                configuredIntervalTicks,
                phase,
                scheduledCensuses,
                cacheDrivenRebuilds,
                skippedCensuses,
                changedKeys,
                callbackDispatches,
                totalRefreshNanos,
                maximumRefreshNanos,
                lastRefreshNanos);
    }

    public synchronized void resetCounters() {
        ticksObserved = 0;
        scheduledCensuses = 0;
        cacheDrivenRebuilds = 0;
        skippedCensuses = 0;
        changedKeys = 0;
        callbackDispatches = 0;
        totalRefreshNanos = 0;
        maximumRefreshNanos = 0;
        lastRefreshNanos = 0;
        lastLogTick = 0;
    }
}
