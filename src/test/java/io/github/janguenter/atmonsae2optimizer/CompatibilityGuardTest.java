package io.github.janguenter.atmonsae2optimizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CompatibilityGuardTest {
    @Test
    void pinnedAe2SignaturesRemainPresent() {
        assertDoesNotThrow(CompatibilityGuard::verifyAe2Signatures);
    }
}
