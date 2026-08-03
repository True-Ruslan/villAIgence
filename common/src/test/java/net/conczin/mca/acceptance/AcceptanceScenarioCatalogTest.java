package net.conczin.mca.acceptance;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcceptanceScenarioCatalogTest {
    private static final List<String> EXPECTED_COLUMNS = List.of(
            "id",
            "domain",
            "severity",
            "state",
            "layer",
            "gate",
            "invariant",
            "oracle",
            "timeoutSeconds",
            "evidence",
            "manualRationale"
    );

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
        List<Map<String, String>> scenarios = loadCatalog();
        assertTrue(scenarios.size() >= REQUIRED_DOMAINS.size() * 3,
                "The initial catalog must include at least three scenarios per risk domain");

        Set<String> ids = new HashSet<>();
        Map<String, Integer> domainCounts = new HashMap<>();

        for (Map<String, String> scenario : scenarios) {
            String id = required(scenario, "id");
            assertTrue(id.matches("VAI-[A-Z]+-[0-9]{3}"),
                    () -> "Scenario ID must be stable and machine-readable: " + id);
            assertTrue(ids.add(id), () -> "Duplicate scenario ID: " + id);

            String domain = required(scenario, "domain");
            assertTrue(REQUIRED_DOMAINS.contains(domain),
                    () -> "Unknown risk domain for " + id + ": " + domain);
            domainCounts.merge(domain, 1, Integer::sum);

            String severity = required(scenario, "severity");
            assertTrue(VALID_SEVERITIES.contains(severity),
                    () -> "Unknown severity for " + id + ": " + severity);

            String state = required(scenario, "state");
            assertTrue(VALID_STATES.contains(state),
                    () -> "Unknown automation state for " + id + ": " + state);

            required(scenario, "layer");
            required(scenario, "invariant");
            String oracle = required(scenario, "oracle");
            required(scenario, "evidence");

            int timeoutSeconds = positiveInt(scenario, "timeoutSeconds");
            assertTrue(timeoutSeconds <= 1_800,
                    () -> "Scenario timeout exceeds the 30-minute safety ceiling: " + id);

            if (state.equals("AUTOMATED")) {
                required(scenario, "gate");
                assertFalse(oracle.equalsIgnoreCase("manual observation"),
                        () -> "Automated scenario must have a machine-verifiable oracle: " + id);
            }

            if (state.equals("MANUAL_CANARY")) {
                required(scenario, "manualRationale");
            }

            if (severity.equals("CRITICAL") && !state.equals("AUTOMATED")) {
                assertTrue(state.equals("MANUAL_CANARY"),
                        () -> "Critical scenario must be automated or an explicit manual canary: " + id);
                required(scenario, "manualRationale");
            }
        }

        assertEquals(REQUIRED_DOMAINS, domainCounts.keySet(),
                "Every architectural risk domain must be represented");
        domainCounts.forEach((domain, count) -> assertTrue(count >= 3,
                () -> "Risk domain requires at least three scenarios: " + domain));
    }

    private static List<Map<String, String>> loadCatalog() throws IOException {
        try (InputStream stream = AcceptanceScenarioCatalogTest.class
                .getResourceAsStream("/acceptance/scenarios.tsv")) {
            assertNotNull(stream,
                    "Missing /acceptance/scenarios.tsv risk-based acceptance catalog");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String headerLine = reader.readLine();
                assertNotNull(headerLine, "Acceptance catalog must contain a header");
                List<String> columns = split(headerLine);
                assertEquals(EXPECTED_COLUMNS, columns,
                        "Acceptance catalog header must use the canonical column order");

                List<Map<String, String>> rows = new ArrayList<>();
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }

                    List<String> values = split(line);
                    assertEquals(EXPECTED_COLUMNS.size(), values.size(),
                            "Acceptance catalog line " + lineNumber + " has the wrong column count");

                    Map<String, String> row = new HashMap<>();
                    for (int index = 0; index < EXPECTED_COLUMNS.size(); index++) {
                        row.put(EXPECTED_COLUMNS.get(index), values.get(index).trim());
                    }
                    rows.add(Map.copyOf(row));
                }
                return List.copyOf(rows);
            }
        }
    }

    private static List<String> split(String line) {
        return Arrays.asList(line.split("\\t", -1));
    }

    private static String required(Map<String, String> row, String field) {
        String value = row.get(field);
        assertNotNull(value, () -> "Missing required field: " + field);
        assertFalse(value.isBlank(), () -> "Field must not be blank: " + field);
        return value;
    }

    private static int positiveInt(Map<String, String> row, String field) {
        String value = required(row, field);
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new AssertionError("Field must be an integer: " + field, exception);
        }
        assertTrue(number > 0, () -> "Field must be positive: " + field);
        return number;
    }
}
