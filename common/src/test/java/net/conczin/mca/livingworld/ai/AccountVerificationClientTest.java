package net.conczin.mca.livingworld.ai;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountVerificationClientTest {
    @Test
    void derivesFixedVerificationPathOnTrustedOrigin() {
        ProviderEndpoint chat = ProviderEndpointPolicy.parse(
                "https://api.conczin.net/v1/mca/chat?ignored=true",
                false
        );

        URI uri = AccountVerificationClient.buildVerificationUri(
                chat,
                "sample@test.invalid",
                "Player Name"
        );

        assertEquals("https", uri.getScheme());
        assertEquals("api.conczin.net", uri.getHost());
        assertEquals("/v1/mca/verify", uri.getPath());
        assertEquals("email=sample%40test.invalid&player=Player+Name", uri.getRawQuery());
    }

    @Test
    void rejectsLookalikeAndPlainHttpOrigins() {
        ProviderEndpoint lookalike = ProviderEndpointPolicy.parse(
                "https://conczin.net.example.invalid/v1/mca/chat",
                false
        );
        ProviderEndpoint forgedHttp = new ProviderEndpoint(
                URI.create("http://api.conczin.net/v1/mca/chat"),
                "api.conczin.net",
                ProviderEndpoint.Family.CONCZIN,
                false,
                true
        );

        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationClient.buildVerificationUri(lookalike, "a", "b"));
        assertThrows(IllegalArgumentException.class, () ->
                AccountVerificationClient.buildVerificationUri(forgedHttp, "a", "b"));
    }

    @Test
    void parsesOnlyDocumentedVerificationAnswers() {
        assertEquals(AccountVerificationClient.Result.SUCCESS,
                AccountVerificationClient.parseResult("{\"answer\":\"success\"}"));
        assertEquals(AccountVerificationClient.Result.FAILED,
                AccountVerificationClient.parseResult("{\"answer\":\"failed\"}"));
        assertEquals(AccountVerificationClient.Result.ERROR,
                AccountVerificationClient.parseResult("{\"answer\":\"other\"}"));
        assertEquals(AccountVerificationClient.Result.ERROR,
                AccountVerificationClient.parseResult("not json"));
    }
}
