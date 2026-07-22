package net.conczin.mca.livingworld.memory2;

/** One bounded recent dialogue message held only for the current AI turn. */
public record WorkingMemoryMessage(String role, String content) {
    public WorkingMemoryMessage {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("role is required");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("content is required");
        role = role.strip();
        if (!role.equals("user") && !role.equals("assistant")) {
            throw new IllegalArgumentException("unsupported dialogue role: " + role);
        }
        content = content.strip();
    }
}
