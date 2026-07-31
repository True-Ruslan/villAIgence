package net.conczin.mca.livingworld.ai;

import java.net.URI;
import java.util.Objects;

/**
 * Validated, normalized provider destination.
 *
 * <p>The URI is safe to use only after construction through {@link ProviderEndpointPolicy}.</p>
 */
public record ProviderEndpoint(
        URI uri,
        String host,
        Family family,
        boolean loopback,
        boolean trustedConczin
) {
    public ProviderEndpoint {
        uri = Objects.requireNonNull(uri, "uri");
        host = Objects.requireNonNull(host, "host");
        family = Objects.requireNonNull(family, "family");
    }

    public String externalForm() {
        return uri.toASCIIString();
    }

    public enum Family {
        OPENAI,
        OPENROUTER,
        CONCZIN,
        CUSTOM
    }
}
