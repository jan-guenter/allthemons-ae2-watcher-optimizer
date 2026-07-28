package io.github.janguenter.atmonsae2optimizer.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CensusScheduleTest {
    @Test
    void intervalOneAlwaysRunsAndRestoresStockScheduling() {
        var phase = CensusSchedule.phaseFor("Grid #7@minecraft:overworld", 1, true);

        assertEquals(0, phase);
        for (var tick = 0; tick < 100; tick++) {
            assertTrue(CensusSchedule.shouldRun(tick, 1, phase));
        }
    }

    @Test
    void staggeredPhaseIsDeterministicAndBounded() {
        var identity = "Grid #12@minecraft:overworld";
        var firstPhase = CensusSchedule.phaseFor(identity, 10, true);
        var secondPhase = CensusSchedule.phaseFor(identity, 10, true);

        assertEquals(firstPhase, secondPhase);
        assertTrue(firstPhase >= 0 && firstPhase < 10);
        assertTrue(CensusSchedule.shouldRun(firstPhase, 10, firstPhase));
        assertTrue(CensusSchedule.shouldRun(firstPhase + 10L, 10, firstPhase));
        assertFalse(CensusSchedule.shouldRun(firstPhase + 1L, 10, firstPhase));
    }

    @Test
    void disablingStaggerUsesSharedZeroPhase() {
        assertEquals(0, CensusSchedule.phaseFor("Grid #1", 20, false));
        assertEquals(0, CensusSchedule.phaseFor("Grid #2", 20, false));
        assertTrue(CensusSchedule.shouldRun(0, 20, 0));
        assertTrue(CensusSchedule.shouldRun(20, 20, 0));
    }

    @Test
    void eachPhaseRunsExactlyOncePerInterval() {
        for (var phase = 0; phase < 10; phase++) {
            var runs = 0;
            for (var tick = 0; tick < 10; tick++) {
                if (CensusSchedule.shouldRun(tick, 10, phase)) {
                    runs++;
                }
            }
            assertEquals(1, runs);
        }
    }

    @Test
    void invalidInputsFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> CensusSchedule.phaseFor("grid", 0, true));
        assertThrows(IllegalArgumentException.class, () -> CensusSchedule.shouldRun(0, 10, -1));
        assertThrows(IllegalArgumentException.class, () -> CensusSchedule.shouldRun(0, 10, 10));
    }
}
