# LivingWorld Safe Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable MCA's whitelisted NPC AI actions for configured LivingWorld while enforcing server-thread execution.

**Architecture:** A pure policy selects LivingWorld vs legacy MCA tool enablement. Existing `TriggerCommandInfo` instances wrap their mutation callbacks so every action is revalidated and scheduled on the Minecraft server thread.

**Tech Stack:** Java 21, MCA ChatAI/TriggerCommandInfos, JUnit 5.

## Constraints

- No arbitrary commands or unrestricted actions.
- No new mandatory configuration.
- Legacy MCA tool switch remains authoritative without configured LivingWorld.
- World mutation never executes directly on the async LLM thread.

## Tasks

- [x] Define and test tool-enable selection policy.
- [x] Add `safeActionsEnabled=true` default.
- [x] Wire configured LivingWorld into existing MCA tool switch.
- [x] Revalidate and schedule action callbacks on server thread.
- [x] Document whitelist and security boundary.
- [ ] Final compile/static review and merge.
