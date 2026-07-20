# LivingWorld Safe Actions Design

## Goal

Let configured LivingWorld NPCs perform a small, explicit set of useful MCA actions without arbitrary command execution and without mutating Minecraft world state from async AI threads.

## Design

- Reuse MCA's existing `TriggerCommandInfos` whitelist: follow, stay, move freely, armor on/off, go home, trade.
- `safeActionsEnabled=true` is the zero-config LivingWorld default.
- When LivingWorld is configured, its switch controls MCA AI tools; when not configured, legacy `villagerChatAIUseTools` remains authoritative.
- Keep LLM output constrained to the existing structured optional-command field and exact whitelist lookup.
- Wrap each `TriggerCommandInfo.call` so mutation is scheduled on the Minecraft server thread and availability is revalidated immediately before execution.

## Non-goals

No arbitrary slash commands, item spawning, operator commands, scripting, unrestricted block edits, autonomous long-horizon planning, or new action types in this milestone.
