package net.conczin.mca.livingworld.relationship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NpcSocialMutationIdentityTest {
    @Test
    void identityIsDeterministicForExactSourceAndCause() {
        UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID cause = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        UUID first = NpcSocialMutationIdentity.forCause(source, cause);
        UUID replay = NpcSocialMutationIdentity.forCause(source, cause);

        assertEquals(first, replay);
        assertEquals(
                UUID.fromString("9d792d97-37af-3650-8ef7-33acf6d1f274"),
                first,
                "namespace and canonical identity bytes are a persistence contract"
        );
    }

    @Test
    void sourceAndCauseBothParticipateInIdentity() {
        UUID source = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID otherSource = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID cause = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID otherCause = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        UUID baseline = NpcSocialMutationIdentity.forCause(source, cause);

        assertNotEquals(baseline, NpcSocialMutationIdentity.forCause(otherSource, cause));
        assertNotEquals(baseline, NpcSocialMutationIdentity.forCause(source, otherCause));
    }
}
