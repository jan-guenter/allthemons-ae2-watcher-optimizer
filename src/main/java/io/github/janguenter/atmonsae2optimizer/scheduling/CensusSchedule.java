package io.github.janguenter.atmonsae2optimizer.scheduling;

import java.util.Objects;

public final class CensusSchedule {
    private CensusSchedule() {
    }

    public static int phaseFor(String gridIdentity, int intervalTicks, boolean staggerGrids) {
        requirePositiveInterval(intervalTicks);
        Objects.requireNonNull(gridIdentity, "gridIdentity");
        if (!staggerGrids || intervalTicks == 1) {
            return 0;
        }
        return Math.floorMod(gridIdentity.hashCode(), intervalTicks);
    }

    public static boolean shouldRun(long tick, int intervalTicks, int phase) {
        requirePositiveInterval(intervalTicks);
        if (phase < 0 || phase >= intervalTicks) {
            throw new IllegalArgumentException("phase must be in [0, intervalTicks)");
        }
        return Math.floorMod(tick, (long) intervalTicks) == phase;
    }

    private static void requirePositiveInterval(int intervalTicks) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("intervalTicks must be at least 1");
        }
    }
}
