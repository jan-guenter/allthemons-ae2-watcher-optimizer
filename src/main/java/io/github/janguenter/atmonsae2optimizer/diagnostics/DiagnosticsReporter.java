package io.github.janguenter.atmonsae2optimizer.diagnostics;

import io.github.janguenter.atmonsae2optimizer.AtmonsAe2Optimizer;
import io.github.janguenter.atmonsae2optimizer.config.OptimizerConfig;

public final class DiagnosticsReporter {
    private DiagnosticsReporter() {
    }

    public static void watchAllChanged(GridMetrics metrics, boolean gainedWatchAll) {
        if (gainedWatchAll && OptimizerConfig.VALUES.warnOnWatchAll.get()) {
            AtmonsAe2Optimizer.LOGGER.warn(
                    "AE2 grid gained a watch-all watcher (blank/fuzzy emitters are common): {}",
                    metrics.snapshot().diagnosticLine());
        }
    }

    public static void maybeLog(GridMetrics metrics, long tick) {
        if (!OptimizerConfig.VALUES.diagnosticsEnabled.get()) {
            return;
        }
        var interval = OptimizerConfig.VALUES.diagnosticsLogIntervalTicks.get();
        if (metrics.shouldLog(tick, interval)) {
            AtmonsAe2Optimizer.LOGGER.info("AE2 watcher diagnostics: {}", metrics.snapshot().diagnosticLine());
        }
    }
}
