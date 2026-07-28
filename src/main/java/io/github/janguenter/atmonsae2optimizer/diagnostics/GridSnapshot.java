package io.github.janguenter.atmonsae2optimizer.diagnostics;

import java.util.Locale;

public record GridSnapshot(
        String gridId,
        String dimension,
        long ticksObserved,
        int watcherCount,
        int uniqueExactInterestCount,
        int watchAllWatcherCount,
        boolean throttlingActive,
        int configuredIntervalTicks,
        int phase,
        long scheduledCensuses,
        long cacheDrivenRebuilds,
        long skippedCensuses,
        long changedKeys,
        long callbackDispatches,
        long totalRefreshNanos,
        long maximumRefreshNanos,
        long lastRefreshNanos) {

    public String diagnosticLine() {
        var refreshes = scheduledCensuses + cacheDrivenRebuilds;
        var averageMillis = refreshes == 0 ? 0.0 : totalRefreshNanos / 1_000_000.0 / refreshes;
        return String.format(
                Locale.ROOT,
                "%s dim=%s ticks=%d watchers=%d exact=%d watchAll=%d throttle=%s interval=%d phase=%d "
                        + "scheduled=%d cacheDriven=%d skipped=%d changedKeys=%d callbacks=%d "
                        + "refreshMs(avg/max/last)=%.3f/%.3f/%.3f",
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
                averageMillis,
                maximumRefreshNanos / 1_000_000.0,
                lastRefreshNanos / 1_000_000.0);
    }
}
