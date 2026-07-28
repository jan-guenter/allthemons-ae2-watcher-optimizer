package io.github.janguenter.atmonsae2optimizer.diagnostics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DiagnosticsRegistry {
    private static final Map<Object, GridMetrics> METRICS_BY_SERVICE = new WeakHashMap<>();

    private DiagnosticsRegistry() {
    }

    public static void register(Object storageService, GridMetrics metrics) {
        synchronized (METRICS_BY_SERVICE) {
            METRICS_BY_SERVICE.put(storageService, metrics);
        }
    }

    public static List<GridSnapshot> snapshots() {
        synchronized (METRICS_BY_SERVICE) {
            var snapshots = new ArrayList<GridSnapshot>(METRICS_BY_SERVICE.size());
            for (var metrics : METRICS_BY_SERVICE.values()) {
                snapshots.add(metrics.snapshot());
            }
            snapshots.sort(Comparator.comparing(GridSnapshot::gridId));
            return List.copyOf(snapshots);
        }
    }

    public static int resetCounters() {
        synchronized (METRICS_BY_SERVICE) {
            for (var metrics : METRICS_BY_SERVICE.values()) {
                metrics.resetCounters();
            }
            return METRICS_BY_SERVICE.size();
        }
    }
}
