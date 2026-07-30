package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderEndpointPolicyTest {
    @Test
    void normalizesHttpsHostsAndClassifiesKnownProviderFamilies() {
        ProviderEndpoint openAi = ProviderEndpointPolicy.parse(
                "https://API.OPENAI.COM./v1/chat/completions?mode=test",
                false
        );
        ProviderEndpoint openRouter = ProviderEndpointPolicy.parse(
                "https://OpenRouter.AI/api/v1/chat/completions",
                false
        );

        assertEquals("api.openai.com", openAi.host());
        assertEquals("https://api.openai.com/v1/chat/completions?mode=test", openAi.externalForm());
        assertEquals(ProviderEndpoint.Family.OPENAI, openAi.family());
        assertFalse(openAi.loopback());
        assertFalse(openAi.trustedConczin());

        assertEquals("openrouter.ai", openRouter.host());
        assertEquals(ProviderEndpoint.Family.OPENROUTER, openRouter.family());
    }

    @Test
    void remotePlainHttpIsRejectedEvenWhenLoopbackDevelopmentModeIsEnabled() {
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://example.com/v1/chat", true));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://192.168.1.10/v1/chat", true));
    }

    @Test
    void loopbackPlainHttpRequiresExplicitDevelopmentMode() {
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://127.0.0.1:8080/v1/chat", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://localhost:8080/v1/chat", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://[::1]:8080/v1/chat", false));

        assertTrue(ProviderEndpointPolicy.parse(
                "http://127.255.255.255:8080/v1/chat",
                true
        ).loopback());
        assertTrue(ProviderEndpointPolicy.parse(
                "http://LOCALHOST.:8080/v1/chat",
                true
        ).loopback());
        assertTrue(ProviderEndpointPolicy.parse(
                "http://[::1]:8080/v1/chat",
                true
        ).loopback());
    }

    @Test
    void malformedLoopbackLookalikesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://127.0.0.999:8080/v1/chat", true));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://127.0.0.1.example.invalid:8080/v1/chat", true));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("http://localhost.example.invalid:8080/v1/chat", true));
    }

    @Test
    void trustedConczinClassificationUsesARealHostBoundary() {
        ProviderEndpoint exact = ProviderEndpointPolicy.parse("https://conczin.net/v1/chat", false);
        ProviderEndpoint subdomain = ProviderEndpointPolicy.parse("https://api.conczin.net/v1/chat", false);
        ProviderEndpoint lookalike = ProviderEndpointPolicy.parse(
                "https://conczin.net.example.invalid/v1/chat",
                false
        );

        assertTrue(exact.trustedConczin());
        assertTrue(subdomain.trustedConczin());
        assertEquals(ProviderEndpoint.Family.CONCZIN, subdomain.family());
        assertFalse(lookalike.trustedConczin());
        assertEquals(ProviderEndpoint.Family.CUSTOM, lookalike.family());
    }

    @Test
    void rejectsUnsafeOrAmbiguousUriForms() {
        assertThrows(IllegalArgumentException.class, () -> ProviderEndpointPolicy.parse("", false));
        assertThrows(IllegalArgumentException.class, () -> ProviderEndpointPolicy.parse("not a URI", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("https://user:secret@example.com/v1/chat", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("https://example.com/v1/chat#fragment", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("ftp://example.com/resource", false));
        assertThrows(IllegalArgumentException.class, () ->
                ProviderEndpointPolicy.parse("https://аррӏе.example/v1/chat", false));
    }
}
