package net.conczin.mca.livingworld.admission;

import net.conczin.mca.livingworld.diagnostics.AiOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiAdmissionControllerTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final long SECOND = 1_000_000_000L;

    private final AiAdmissionSettings settings = new AiAdmissionSettings(
            1,
            1,
            1,
            1_000,
            5_000
    );

    @BeforeEach
    void reset() {
        AiAdmissionController.resetForTests();
    }

    @Test
    void firstRequestIsAllowedAndSaturationRejectsImmediately() {
        long now = 10 * SECOND;
        AiAdmissionResult first = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now);
        AiAdmissionResult second = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_B, settings, now);

        assertEquals(AiAdmissionDecision.ALLOWED, first.decision());
        assertNotNull(first.permit());
        assertEquals(AiAdmissionDecision.SATURATED, second.decision());
        assertNull(second.permit());
    }

    @Test
    void closingPermitRestoresCapacityAndDoubleCloseIsSafe() {
        long now = 10 * SECOND;
        AiAdmissionResult first = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now);
        first.permit().close();
        first.permit().close();

        AiAdmissionResult second = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_B, settings, now);
        assertEquals(AiAdmissionDecision.ALLOWED, second.decision());
        assertEquals(1, AiAdmissionController.snapshot(settings, now).chat().active());
    }

    @Test
    void samePlayerSameStageIsCooldownLimitedButStagesAndPlayersAreIndependent() {
        long now = 10 * SECOND;
        AiAdmissionResult first = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now);
        first.permit().close();

        AiAdmissionResult sameStage = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now + 500_000_000L);
        AiAdmissionResult anotherStage = AiAdmissionController.tryAcquire(AiOperation.STT, PLAYER_A, settings, now + 500_000_000L);
        AiAdmissionResult anotherPlayer = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_B, settings, now + 500_000_000L);

        assertEquals(AiAdmissionDecision.PLAYER_COOLDOWN, sameStage.decision());
        assertEquals(AiAdmissionDecision.ALLOWED, anotherStage.decision());
        assertEquals(AiAdmissionDecision.ALLOWED, anotherPlayer.decision());
    }

    @Test
    void playerCooldownExpiresAtConfiguredBoundary() {
        long now = 10 * SECOND;
        AiAdmissionResult first = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now);
        first.permit().close();

        AiAdmissionResult allowed = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now + SECOND);
        assertEquals(AiAdmissionDecision.ALLOWED, allowed.decision());
    }

    @Test
    void providerCooldownRejectsUntilExpiryWithoutConsumingCapacity() {
        long now = 10 * SECOND;
        AiAdmissionController.onRateLimited(AiOperation.STT, 5_000, now);

        AiAdmissionResult blocked = AiAdmissionController.tryAcquire(AiOperation.STT, PLAYER_A, settings, now + 4 * SECOND);
        AiAdmissionResult allowed = AiAdmissionController.tryAcquire(AiOperation.STT, PLAYER_A, settings, now + 5 * SECOND);

        assertEquals(AiAdmissionDecision.PROVIDER_COOLDOWN, blocked.decision());
        assertEquals(AiAdmissionDecision.ALLOWED, allowed.decision());
    }

    @Test
    void laterRateLimitCanExtendButShorterDeadlineCannotReduceCooldown() {
        long now = 10 * SECOND;
        AiAdmissionController.onRateLimited(AiOperation.TTS, 5_000, now);
        AiAdmissionController.onRateLimited(AiOperation.TTS, 1_000, now + SECOND);
        assertEquals(4_000, AiAdmissionController.snapshot(settings, now + SECOND).tts().providerCooldownRemainingMillis());

        AiAdmissionController.onRateLimited(AiOperation.TTS, 5_000, now + 2 * SECOND);
        assertEquals(5_000, AiAdmissionController.snapshot(settings, now + 2 * SECOND).tts().providerCooldownRemainingMillis());
    }

    @Test
    void snapshotReportsActiveCapacityRejectionsAndCooldown() {
        long now = 10 * SECOND;
        AiAdmissionResult active = AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_A, settings, now);
        AiAdmissionController.tryAcquire(AiOperation.CHAT, PLAYER_B, settings, now);
        AiAdmissionController.onRateLimited(AiOperation.STT, 5_000, now);
        AiAdmissionController.tryAcquire(AiOperation.STT, PLAYER_A, settings, now);

        AiAdmissionSnapshot snapshot = AiAdmissionController.snapshot(settings, now + SECOND);

        assertEquals(1, snapshot.chat().active());
        assertEquals(1, snapshot.chat().maxConcurrent());
        assertEquals(1, snapshot.chat().rejected());
        assertEquals(0, snapshot.chat().providerCooldownRemainingMillis());
        assertEquals(0, snapshot.stt().active());
        assertEquals(1, snapshot.stt().rejected());
        assertEquals(4_000, snapshot.stt().providerCooldownRemainingMillis());

        active.permit().close();
    }
}
