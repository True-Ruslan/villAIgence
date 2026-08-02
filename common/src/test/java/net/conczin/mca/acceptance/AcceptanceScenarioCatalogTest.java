package net.conczin.mca.acceptance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceScenarioCatalogTest {
    private static final Set<String> REQUIRED_DOMAINS = Set.of(
            "BOOT_PACKAGE",
            "IDENTITY_LIFECYCLE",
            "PERSISTENCE_IDEMPOTENCY",
            "NAVIGATION_SURVIVAL",
            "GAMEPLAY_INTERACTION",
            "AI_VOICE_RESILIENCE",
            "CONCURRENCY_AUTHORIZATION"
    );

    private static final Set<String> VALID_SEVERITIES = Set.of(
            "CRITICAL", "HIGH", "MEDIUM", "LOW"
    );

    private static final Set<String> VALID_STATES = Set.of(
            "AUTOMATED", "PLANNED", "MANUAL_CANARY"
    );

    @Test
    void catalogCoversEveryRiskDomainWithDeterministicEvidence() throws IOException {
        JsonArray scenarios = loadCatalog();
        assertTrue(scenarios.size() >= REQUIRED_DOMAINS.size() * 3,
                "The initial catalog must include at least three scenarios per risk domain");

        Set<String> ids = new HashSet<>();
        Map<String, Integer> domainCounts = new HashMap<>();

        for (JsonElement element : scenarios) {
            assertTrue(element.isJsonObject(), "Every scenario entry must be a JSON object");
            JsonObject scenario = element.getAsJsonObject();

            String id = requiredString(scenario, "id");
            assertTrue(id.matches("VAI-[A-Z]+-[0-9]{3}"),
                    () -> "Scenario ID must be stable and machine-readable: " + id);
            assertTrue(ids.add(id), () -> "Duplicate scenario ID: " + id);

            String domain = requiredString(scenario, "domain");
            assertTrue(REQUIRED_DOMAINS.contains(domain),
                    () -> "Unknown risk domain for " + id + ": " + domain);
            domainCounts.merge(domain, 1, Integer::sum);

            String severity = requiredString(scenario, "severity");
            assertTrue(VALID_SEVERITIES.contains(severity),
                    () -> "Unknown severity for " + id + ": " + severity);

            String state = requiredString(scenario, "state");
            assertTrue(VALID_STATES.contains(state),
                    () -> "Unknown automation state for " + id + ": " + state);

            requiredString(scenario, "layer");
            requiredString(scenario, "invariant");
            requiredString(scenario, "oracle");
            requiredString(scenario, "evidence");

            int timeoutSeconds = requiredPositiveInt(scenario, "timeoutSeconds");
            assertTrue(timeoutSeconds <= 1_800,
                    () -> "Scenario timeout exceeds the 30-minute safety ceiling: " + id);

            if (state.equals("AUTOMATED")) {
                requiredString(scenario, "gate");
                assertFalse(requiredString(scenario, "oracle").equalsIgnoreCase("manual observation"),
                        () -> "Automated scenario must have a machine-verifiable oracle: " + id);
            }

            if (state.equals("MANUAL_CANARY")) {
                requiredString(scenario, "manualRationale");
            }

            if (severity.equals("CRITICAL") && !state.equals("AUTOMATED")) {
                assertTrue(state.equals("MANUAL_CANARY"),
                        () -> "Critical scenario must be automated or an explicit manual canary: " + id);
                requiredString(scenario, "manualRationale");
            }
        }

        assertEquals(REQUIRED_DOMAINS, domainCounts.keySet(),
                "Every architectural risk domain must be represented");
        domainCounts.forEach((domain, count) -> assertTrue(count >= 3,
                () -> "Risk domain requires at least three scenarios: " + domain));
    }

    private static JsonArray loadCatalog() throws IOException {
        try (InputStream stream = AcceptanceScenarioCatalogTest.class
                .getResourceAsStream("/acceptance/scenarios.json")) {
            assertNotNull(stream,
                    "Missing /acceptance/scenarios.json risk-based acceptance catalog");
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                assertTrue(root.isJsonArray(), "Acceptance catalog root must be a JSON array");
                return root.getAsJsonArray();
            }
        }
    }

    private static String requiredString(JsonObject object, String field) {
        assertTrue(object.has(field), () -> "Missing required field: " + field);
        JsonElement value = object.get(field);
        assertTrue(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString(),
                () -> "Field must be a string: " + field);
        String text = value.getAsString().trim();
        assertFalse(text.isEmpty(), () -> "Field must not be blank: " + field);
        return text;
    }

    private static int requiredPositiveInt(JsonObject object, String field) {
        assertTrue(object.has(field), () -> "Missing required field: " + field);
        JsonElement value = object.get(field);
        assertTrue(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber(),
                () -> "Field must be a number: " + field);
        int number = value.getAsInt();
        assertTrue(number > 0, () -> "Field must be positive: " + field);
        return number;
    }
}
