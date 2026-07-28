package io.github.janguenter.atmonsae2optimizer.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridMetricsTest {
    @Test
    void recordsScheduledAndCacheDrivenRefreshesSeparately() {
        var metrics = new GridMetrics();
        metrics.identify("Grid #3", "minecraft:overworld");

        assertFalse(metrics.observe(4, 2, 0, true, 10, 3));
        metrics.recordSkippedCensus();
        metrics.recordRefresh(true, 2_000_000);
        metrics.recordRefresh(false, 4_000_000);
        metrics.recordNotification(3);

        var snapshot = metrics.snapshot();
        assertEquals("Grid #3", snapshot.gridId());
        assertEquals("minecraft:overworld", snapshot.dimension());
        assertEquals(1, snapshot.ticksObserved());
        assertEquals(4, snapshot.watcherCount());
        assertEquals(2, snapshot.uniqueExactInterestCount());
        assertEquals(1, snapshot.scheduledCensuses());
        assertEquals(1, snapshot.cacheDrivenRebuilds());
        assertEquals(1, snapshot.skippedCensuses());
        assertEquals(1, snapshot.changedKeys());
        assertEquals(3, snapshot.callbackDispatches());
        assertEquals(6_000_000, snapshot.totalRefreshNanos());
        assertEquals(4_000_000, snapshot.maximumRefreshNanos());
        assertEquals(4_000_000, snapshot.lastRefreshNanos());
    }

    @Test
    void watchAllTransitionIsReportedOnlyWhenItAppears() {
        var metrics = new GridMetrics();

        assertTrue(metrics.observe(1, 0, 1, true, 10, 0));
        assertFalse(metrics.observe(1, 0, 1, true, 10, 0));
        assertFalse(metrics.observe(1, 0, 0, true, 10, 0));
        assertTrue(metrics.observe(1, 0, 2, true, 10, 0));
    }

    @Test
    void firstResolvedIdentityWinsAndDrivesTheScheduleSeed() {
        var metrics = new GridMetrics();

        metrics.identify("Grid #8", "minecraft:the_nether");
        metrics.identify("Grid #99", "minecraft:the_end");

        assertEquals("Grid #8@minecraft:the_nether", metrics.scheduleIdentity());
        assertEquals("Grid #8", metrics.snapshot().gridId());
    }

    @Test
    void resetKeepsCurrentClassificationButClearsCounters() {
        var metrics = new GridMetrics();
        metrics.observe(2, 3, 1, true, 5, 4);
        metrics.recordRefresh(true, 100);
        metrics.recordNotification(2);

        metrics.resetCounters();

        var snapshot = metrics.snapshot();
        assertEquals(0, snapshot.ticksObserved());
        assertEquals(2, snapshot.watcherCount());
        assertEquals(3, snapshot.uniqueExactInterestCount());
        assertEquals(1, snapshot.watchAllWatcherCount());
        assertEquals(0, snapshot.scheduledCensuses());
        assertEquals(0, snapshot.changedKeys());
    }
}
