package net.conczin.mca.livingworld.memory;

import java.util.Objects;

/** A single role/content message persisted for an NPC/player conversation. */
public record MemoryMessage(String role, String content) {
    public MemoryMessage {
        role = Objects.requireNonNullElse(role, "");
        content = Objects.requireNonNullElse(content, "");
    }
}
