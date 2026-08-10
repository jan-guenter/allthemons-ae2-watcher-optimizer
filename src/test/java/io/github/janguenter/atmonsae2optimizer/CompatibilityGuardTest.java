package io.github.janguenter.atmonsae2optimizer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class CompatibilityGuardTest {
    @Test
    void pinnedAe2SignaturesRemainPresent() {
        assertDoesNotThrow(CompatibilityGuard::verifyAe2Signatures);
    }

    @Test
    void extendedAeVersionMatchesTheReviewedPackVersion() throws ReflectiveOperationException {
        Field expectedVersion = CompatibilityGuard.class.getDeclaredField("EXPECTED_EXTENDED_AE");
        expectedVersion.setAccessible(true);

        assertEquals("1.21-2.2.35-neoforge", expectedVersion.get(null));
    }
}
