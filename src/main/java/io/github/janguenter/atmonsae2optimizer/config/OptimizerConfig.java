package io.github.janguenter.atmonsae2optimizer.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class OptimizerConfig {
    public static final Values VALUES;
    private static final ModConfigSpec SPEC;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    private OptimizerConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    public static final class Values {
        public final ModConfigSpec.BooleanValue intervalThrottlingEnabled;
        public final ModConfigSpec.IntValue fullCensusIntervalTicks;
        public final ModConfigSpec.BooleanValue staggerGrids;
        public final ModConfigSpec.BooleanValue diagnosticsEnabled;
        public final ModConfigSpec.IntValue diagnosticsLogIntervalTicks;
        public final ModConfigSpec.BooleanValue warnOnWatchAll;

        private Values(ModConfigSpec.Builder builder) {
            builder.push("watcherCensus");
            intervalThrottlingEnabled = builder
                    .comment(
                            "Throttle AE2's stock full watcher census.",
                            "false restores AE2's stock every-tick scheduling.")
                    .define("intervalThrottlingEnabled", true);
            fullCensusIntervalTicks = builder
                    .comment(
                            "Ticks between stock full censuses while storage watchers exist.",
                            "1 restores AE2's stock scheduling.")
                    .defineInRange("fullCensusIntervalTicks", 10, 1, 1200);
            staggerGrids = builder
                    .comment("Give each grid a deterministic phase so full censuses do not all run together.")
                    .define("staggerGrids", true);
            builder.pop();

            builder.push("diagnostics");
            diagnosticsEnabled = builder
                    .comment(
                            "Periodically log per-grid watcher and census diagnostics.",
                            "In-memory command counters remain available when this is false.")
                    .define("diagnosticsEnabled", true);
            diagnosticsLogIntervalTicks = builder
                    .comment("Ticks between per-grid diagnostic log entries.")
                    .defineInRange("diagnosticsLogIntervalTicks", 1200, 20, 72000);
            warnOnWatchAll = builder
                    .comment("Warn when a grid first gains a fuzzy, blank, or other watch-all watcher.")
                    .define("warnOnWatchAll", true);
            builder.pop();
        }
    }
}
