# LivingWorld safe NPC actions

Configured LivingWorld servers enable MCA's existing hard-coded AI action whitelist by default.

## Available actions

- follow the player
- stay here
- move freely again
- wear armor
- remove armor
- try to go home
- open the trade window when available

The LLM cannot execute arbitrary Minecraft commands, console commands, Java code, or an unrestricted action name. Every requested action is matched against `TriggerCommandInfos` and its current availability predicate.

## Thread safety

LLM requests run asynchronously, but world mutation does not. `TriggerCommandInfo` schedules the action onto the Minecraft server thread and re-checks availability immediately before execution.

## Configuration

`safeActionsEnabled=true` is the LivingWorld default. Set it to `false` to keep AI dialogue/memory/voice while disabling AI-triggered actions.

When LivingWorld is not configured, the original MCA `villagerChatAIUseTools` behavior remains authoritative.
