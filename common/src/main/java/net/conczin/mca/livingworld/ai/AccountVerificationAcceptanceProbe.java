package net.conczin.mca.livingworld.ai;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Explicit operator probe for exercising the production verification transport against a local
 * acceptance harness. This class is never invoked by Minecraft startup or normal command handling.
 */
public final class AccountVerificationAcceptanceProbe {
    private static final int DEFAULT_TIMEOUT_MILLIS = 5_000;

    private AccountVerificationAcceptanceProbe() {
    }

    static URI validateLoopbackUri(String value) {
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException("Probe target must be a valid URI", e);
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Probe target must use HTTP or HTTPS");
        }
        if (uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Probe target must not contain user-info or a fragment");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Probe target must contain a literal loopback host");
        }
        host = stripIpv6Brackets(host);
        if (!isNumericIpLiteral(host)) {
            throw new IllegalArgumentException("Probe target host must be a literal IP address");
        }
        try {
            if (!InetAddress.getByName(host).isLoopbackAddress()) {
                throw new IllegalArgumentException("Probe target host must be loopback");
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Probe target host is invalid", e);
        }
        return uri;
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Usage: AccountVerificationAcceptanceProbe <loopback-uri> [connect-ms] [read-ms]");
            System.exit(2);
            return;
        }

        try {
            URI uri = validateLoopbackUri(args[0]);
            int connectTimeout = args.length >= 2
                    ? parsePositiveTimeout(args[1], "connect-ms")
                    : DEFAULT_TIMEOUT_MILLIS;
            int readTimeout = args.length >= 3
                    ? parsePositiveTimeout(args[2], "read-ms")
                    : DEFAULT_TIMEOUT_MILLIS;
            try {
                AccountVerificationTransport.Response response = AccountVerificationTransport.execute(
                        uri,
                        connectTimeout,
                        readTimeout
                );
                String outcome = response.success() ? "SUCCESS" : "HTTP_ERROR";
                System.out.println(toJson(uri, outcome, response.status(), null));
            } catch (BoundedResponseReader.ResponseTooLargeException e) {
                System.out.println(toJson(uri, "TOO_LARGE", -1, e.getClass().getSimpleName()));
            } catch (BoundedResponseReader.ResponseDeadlineExceededException e) {
                System.out.println(toJson(uri, "DEADLINE", -1, e.getClass().getSimpleName()));
            } catch (IOException e) {
                System.out.println(toJson(uri, "IO_ERROR", -1, e.getClass().getSimpleName()));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Verification probe rejected unsafe or invalid arguments: " + e.getMessage());
            System.exit(2);
        }
    }

    private static int parsePositiveTimeout(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) throw new NumberFormatException("non-positive");
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a positive integer", e);
        }
    }

    private static String toJson(URI uri, String outcome, int status, String errorType) {
        return "{"
                + "\"marker\":\"VILLAIGENCE_VERIFICATION_PROBE\","
                + "\"outcome\":\"" + outcome + "\","
                + "\"status\":" + status + ","
                + "\"errorType\":" + (errorType == null ? "null" : "\"" + errorType + "\"") + ","
                + "\"target\":\"" + jsonEscape(redactedTarget(uri)) + "\""
                + "}";
    }

    private static String redactedTarget(URI uri) {
        String host = uri.getHost();
        String authorityHost = host != null && host.contains(":") ? "[" + stripIpv6Brackets(host) + "]" : host;
        String authority = authorityHost == null ? "<invalid>" : authorityHost;
        if (uri.getPort() >= 0) authority += ":" + uri.getPort();
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) path = "/";
        return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + authority + path;
    }

    private static String stripIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean isNumericIpLiteral(String host) {
        if (host.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")) return true;
        return host.contains(":") && host.matches("[0-9A-Fa-f:]+");
    }

    private static String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
