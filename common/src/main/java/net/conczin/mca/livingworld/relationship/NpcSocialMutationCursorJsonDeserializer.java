package net.conczin.mca.livingworld.relationship;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Decodes one persisted causal frontier without allowing one malformed cursor to invalidate the whole graph file.
 * Invalid payloads become a deliberately inconsistent sentinel that the store sanitizer will keep fail-closed.
 */
final class NpcSocialMutationCursorJsonDeserializer implements JsonDeserializer<NpcSocialMutationCursor> {
    private static final UUID SENTINEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID FALLBACK_ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public NpcSocialMutationCursor deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
    ) {
        if (json == null || json.isJsonNull() || !json.isJsonObject()) {
            return sentinel(null, null, null, 0L);
        }

        JsonObject object = json.getAsJsonObject();
        UUID source = uuid(object, "sourceNpcId");
        UUID target = uuid(object, "targetNpcId");
        UUID cause = uuid(object, "causeEventId");
        long gameTime = nonNegativeLong(object, "causeGameTime");

        try {
            UUID mutation = uuid(object, "mutationId");
            NpcSocialDelta requested = context.deserialize(
                    object.get("boundedRequestedDelta"),
                    NpcSocialDelta.class
            );
            NpcSocialDelta applied = context.deserialize(
                    object.get("appliedDelta"),
                    NpcSocialDelta.class
            );
            NpcSocialState before = context.deserialize(object.get("before"), NpcSocialState.class);
            NpcSocialState after = context.deserialize(object.get("after"), NpcSocialState.class);
            NpcSocialMutationCursor.Outcome outcome = outcome(object, "outcome");
            if (mutation == null
                    || source == null
                    || target == null
                    || cause == null
                    || requested == null
                    || applied == null
                    || before == null
                    || after == null
                    || outcome == null) {
                return sentinel(source, target, cause, gameTime);
            }
            return new NpcSocialMutationCursor(
                    mutation,
                    source,
                    target,
                    cause,
                    gameTime,
                    requested,
                    applied,
                    before,
                    after,
                    outcome
            );
        } catch (RuntimeException ignored) {
            return sentinel(source, target, cause, gameTime);
        }
    }

    private static NpcSocialMutationCursor sentinel(
            UUID source,
            UUID target,
            UUID cause,
            long gameTime
    ) {
        UUID safeSource = source == null ? SENTINEL_ID : source;
        UUID safeTarget = target;
        if (safeTarget == null || safeTarget.equals(safeSource)) {
            safeTarget = safeSource.equals(FALLBACK_ENTITY_ID)
                    ? SENTINEL_ID
                    : FALLBACK_ENTITY_ID;
        }
        UUID safeCause = cause == null ? SENTINEL_ID : cause;
        return new NpcSocialMutationCursor(
                SENTINEL_ID,
                safeSource,
                safeTarget,
                safeCause,
                gameTime,
                NpcSocialDelta.NONE,
                NpcSocialDelta.NONE,
                NpcSocialState.NEUTRAL,
                NpcSocialState.NEUTRAL,
                NpcSocialMutationCursor.Outcome.NO_CHANGE
        );
    }

    private static UUID uuid(JsonObject object, String field) {
        try {
            JsonElement value = object.get(field);
            if (value == null || value.isJsonNull()) return null;
            return UUID.fromString(value.getAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static long nonNegativeLong(JsonObject object, String field) {
        try {
            JsonElement value = object.get(field);
            if (value == null || value.isJsonNull()) return 0L;
            return Math.max(0L, value.getAsLong());
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static NpcSocialMutationCursor.Outcome outcome(JsonObject object, String field) {
        try {
            JsonElement value = object.get(field);
            if (value == null || value.isJsonNull()) return null;
            return NpcSocialMutationCursor.Outcome.valueOf(value.getAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
