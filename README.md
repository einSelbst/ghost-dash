# Ghost Dash

Ghost Dash recreates the deliberate, server-side version of the Ghost Spear
effect seen on exploit-oriented Minecraft servers. It targets **Paper 1.21.11**
and needs no client mod.

## Gameplay

1. Hold a spear enchanted with **Lunge**.
2. Sneak and press the swap-hands key (`F`) to enter Ghost state.
3. Your visible mannequin (the **vessel**) remains at the starting point while
   your real player becomes hidden from everyone else.
4. Move normally. Wind charges remain active during Ghost state, including
   their knockback boost, while the server records your real route once per tick.
5. Land a spear hit on a living entity. The vessel replays the recorded route
   in a fast time-lapse and the stored hit resolves at the end.

If another player attacks the vessel, the attack is forwarded through
Minecraft's normal damage pipeline, but Ghost state and the recorded route
continue. Only death ends the active dash.

## Safety and balancing

- Maximum Ghost duration and route length
- Independent visual replay speed and combat damage
- Configurable raw-damage cap (default: 20 points / 10 hearts before armor)
- One stored hit per dash
- Cooldown and optional player allowlist
- Cleanup on death, quit, world change, reload, or plugin disable
- `/ghostdash cancel` as a player kill switch
- Player notifications use the action bar and do not write to chat

The time-lapse is intentionally visual. Its artificial speed never multiplies
damage. Damage is derived from the actually recorded route and then capped;
armor, enchantments, absorption, and totems remain in the normal server damage
path.

## Build

Requirements: JDK 21.

```bash
./gradlew clean test build
```

The server JAR is written to `build/libs/GhostDash-0.1.2.jar`.

## Install

1. Stop the Paper server and create a fresh world/config backup.
2. Copy the JAR into `plugins/`.
3. Start the server once to create `plugins/GhostDash/config.yml`.
4. Review the limits, then test with two players before normal use.

Commands:

- `/ghostdash status`
- `/ghostdash cancel`
- `/ghostdash reload` (`ghostdash.admin`)

Permissions:

- `ghostdash.use` — use Ghost Dash (default: true)
- `ghostdash.admin` — reload the configuration (default: op)

## Compatibility

The first release is deliberately pinned to Paper 1.21.11 / Java 21 because
the target server cannot yet move past that client generation. A later Paper
26.x build will need a separate compatibility pass.
