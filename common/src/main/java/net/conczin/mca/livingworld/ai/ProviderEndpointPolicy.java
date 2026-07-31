package net.conczin.mca.livingworld.ai;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Central security policy for Chat, STT and TTS provider destinations. */
public final class ProviderEndpointPolicy {
    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    private ProviderEndpointPolicy() {
    }

    public static ProviderEndpoint parse(String endpoint, boolean allowInsecureLoopback) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Provider endpoint is not configured");
        }

        URI parsed;
        try {
            parsed = new URI(endpoint.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Provider endpoint is not a valid URI", e);
        }

        String scheme = normalizeScheme(parsed.getScheme());
        if (!HTTPS.equals(scheme) && !HTTP.equals(scheme)) {
            throw new IllegalArgumentException("Provider endpoint scheme must be HTTPS or loopback HTTP");
        }
        if (parsed.getUserInfo() != null) {
            throw new IllegalArgumentException("Provider endpoint must not contain user info");
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("Provider endpoint must not contain a fragment");
        }

        String host = normalizeHost(parsed.getHost());
        boolean loopback = isLoopbackHost(host);
        if (HTTP.equals(scheme) && (!allowInsecureLoopback || !loopback)) {
            throw new IllegalArgumentException("Plain HTTP provider endpoints are allowed only for explicit loopback development mode");
        }

        ProviderEndpoint.Family family = classify(host);
        URI normalizedUri = rebuild(parsed, scheme, host);
        return new ProviderEndpoint(
                normalizedUri,
                host,
                family,
                loopback,
                family == ProviderEndpoint.Family.CONCZIN
        );
    }

    private static String normalizeScheme(String scheme) {
        return scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
    }

    private static String normalizeHost(String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            throw new IllegalArgumentException("Provider endpoint must contain a host");
        }

        String host = rawHost.trim();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("Provider endpoint must contain a host");
        }

        if (host.indexOf(':') >= 0) {
            return host.toLowerCase(Locale.ROOT);
        }

        try {
            return IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Provider endpoint host is invalid", e);
        }
    }

    private static boolean isLoopbackHost(String host) {
        if ("localhost".equals(host)
                || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host)) {
            return true;
        }

        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String octet : octets) {
            if (octet.isEmpty() || !octet.chars().allMatch(Character::isDigit)) return false;
            if (octet.length() > 1 && octet.charAt(0) == '0') return false;
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) return false;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return "127".equals(octets[0]);
    }

    private static ProviderEndpoint.Family classify(String host) {
        if (matchesHostBoundary(host, "api.openai.com")) return ProviderEndpoint.Family.OPENAI;
        if (matchesHostBoundary(host, "openrouter.ai")) return ProviderEndpoint.Family.OPENROUTER;
        if (matchesHostBoundary(host, "conczin.net")) return ProviderEndpoint.Family.CONCZIN;
        return ProviderEndpoint.Family.CUSTOM;
    }

    private static boolean matchesHostBoundary(String host, String trustedHost) {
        return host.equals(trustedHost) || host.endsWith("." + trustedHost);
    }

    private static URI rebuild(URI parsed, String scheme, String host) {
        StringBuilder normalized = new StringBuilder(scheme).append("://");
        if (host.indexOf(':') >= 0) {
            normalized.append('[').append(host).append(']');
        } else {
            normalized.append(host);
        }
        if (parsed.getPort() >= 0) {
            normalized.append(':').append(parsed.getPort());
        }
        if (parsed.getRawPath() != null) {
            normalized.append(parsed.getRawPath());
        }
        if (parsed.getRawQuery() != null) {
            normalized.append('?').append(parsed.getRawQuery());
        }

        try {
            return new URI(normalized.toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Provider endpoint cannot be normalized safely", e);
        }
    }
}
