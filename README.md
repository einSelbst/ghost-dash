# GhostDash

[![Latest release](https://img.shields.io/github/v/release/einSelbst/ghost-dash?label=release)](https://github.com/einSelbst/ghost-dash/releases/latest)
[![Build](https://github.com/einSelbst/ghost-dash/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/einSelbst/ghost-dash/actions/workflows/build.yml)
[![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-3498db)](https://papermc.io/)
[![Java 21](https://img.shields.io/badge/Java-21-f89820)](https://adoptium.net/)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**A server-side Ghost Spear / Shadow Dash combat ability for Paper 1.21.11.**

Leave a visible vessel behind, move unseen with wind-charge boosts, then replay
your route as a fast time-lapse and deliver a distance-scaled spear hit. The
entire mechanic runs on the server: players need no client mod or resource pack.

## Features

- Ghost Spear / Shadow Dash gameplay without client-server desync exploits
- A visible mannequin vessel that keeps the player's skin and equipment
- Hidden movement with working wind-charge knockback and boosts
- Fast time-lapse replay with particles and afterimages
- Distance-scaled spear damage with configurable limits
- Normal armor, enchantment, absorption, and totem handling
- Configurable duration, route length, replay speed, cooldown, and allowlist
- Action-bar feedback without plugin messages in chat
- Safe cleanup on death, disconnect, world change, reload, or shutdown

## Requirements

| Component | Requirement |
| --- | --- |
| Server | Paper 1.21.11 |
| Java | 21 |
| Client mod | None |
| Dependencies | None |

GhostDash uses APIs specific to Paper and Minecraft 1.21.11. Spigot, older
Minecraft versions, and Paper 26.x are not currently supported.

## Installation

1. Download `GhostDash-x.y.z.jar` from the [latest release](https://github.com/einSelbst/ghost-dash/releases/latest).
2. Stop the Paper server and create a backup.
3. Copy the JAR into the server's `plugins/` directory.
4. Start the server. GhostDash creates `plugins/GhostDash/config.yml`.
5. Review the damage and timing values before enabling the ability for normal play.

When updating, keep in mind that Bukkit does not overwrite an existing
`config.yml`. Compare it with the configuration in the new release when defaults
change.

## How to use Ghost Dash

1. Hold a spear enchanted with **Lunge** in the main hand.
2. Sneak and press the swap-hands key (`F` by default).
3. A vessel remains at the starting position while the real player becomes
   hidden from other players.
4. Move normally. Wind charges can be used to boost the recorded route.
5. Hit a living entity with the spear.
6. The vessel replays the route in a fast time-lapse. The stored hit resolves
   when the replay reaches the endpoint.

Only one hit can be stored per dash. If the target is no longer within the
configured lock range at the endpoint, the hit expires without damage.

### The vessel

Other players can attack the vessel. That damage is forwarded to the hidden
player through Minecraft's normal damage handling, but the active dash and its
recorded route continue. Death still ends the dash.

## Damage model

GhostDash calculates raw damage from the recorded route:

```text
raw damage = min(maximum damage, base damage + route distance × damage per block)
```

With the default settings, a 10-block route produces 23 damage points (11.5
hearts) before armor, while routes of about 35 blocks or more reach the
60-point cap. Two damage points equal one heart.

Armor, enchantments, absorption, and totems are applied afterward. The action
bar reports both the raw value and the target's effective health loss. If Paper
blocks the player-attributed synthetic hit, GhostDash retries it through a
generic Minecraft damage source; this fallback may not credit the kill to the
attacking player.

## Configuration

The default configuration is stored in
[`src/main/resources/config.yml`](src/main/resources/config.yml).

| Setting | Default | Description |
| --- | ---: | --- |
| `activation.require-lunge` | `true` | Require the Lunge enchantment |
| `activation.minimum-food-level` | `6` | Minimum food level needed to activate |
| `activation.allowed-player-names` | `[]` | Empty allows everyone; otherwise acts as an allowlist |
| `limits.max-duration-seconds` | `8.0` | Maximum time spent recording a route |
| `limits.max-route-distance-blocks` | `64.0` | Maximum recorded route distance |
| `limits.cooldown-seconds` | `20.0` | Cooldown after a completed or cancelled dash |
| `replay.duration-ticks` | `12` | Duration of the visible time-lapse |
| `replay.afterimage-every-ticks` | `2` | Interval between afterimages |
| `replay.afterimage-lifetime-ticks` | `4` | Lifetime of each afterimage |
| `replay.target-lock-range-blocks` | `12.0` | Maximum target distance from the endpoint |
| `damage.base-raw-damage` | `8.0` | Damage before route scaling |
| `damage.raw-damage-per-route-block` | `1.5` | Added damage per recorded block |
| `damage.maximum-raw-damage` | `60.0` | Raw-damage safety cap |
| `damage.vessel-damage-multiplier` | `1.0` | Damage forwarded from the vessel |
| `effects.show-route-particles` | `true` | Show particles during replay |
| `effects.show-afterimages` | `true` | Show mannequin afterimages |

Run `/ghostdash reload` after changing the configuration. Reloading safely ends
active dashes.

## Commands and permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/ghostdash status` | Show readiness, cooldown, or active route information | None |
| `/ghostdash cancel` | Cancel your active dash and return to the vessel | None |
| `/ghostdash reload` | Reload the configuration | `ghostdash.admin` |

| Permission | Default | Description |
| --- | --- | --- |
| `ghostdash.use` | Everyone | Activate and control Ghost Dash |
| `ghostdash.admin` | Server operators | Reload the configuration |

Player-facing responses appear in the action bar, not in chat.

## Troubleshooting

**Ghost Dash does not activate**

- Confirm that the server is running Paper 1.21.11 on Java 21.
- Hold a Lunge-enchanted spear in the main hand, sneak, and press `F`.
- Check the minimum food level, cooldown, allowlist, and `ghostdash.use` permission.

**The stored hit says the target escaped**

- The target moved farther than `replay.target-lock-range-blocks` from the
  recorded endpoint. Increase the value or keep the target closer.

**The damage looks lower than the raw value**

- The raw value is calculated before armor, enchantments, absorption, and
  totems. The action bar also shows the effective health loss.

For reproducible bugs, [open an issue](https://github.com/einSelbst/ghost-dash/issues)
and include the GhostDash version, Paper build, Java version, relevant log
lines, steps to reproduce, and expected behavior.

## Building from source

JDK 21 is required. The included Gradle wrapper downloads the remaining build
dependencies.

```bash
./gradlew clean test build
```

The server JAR is written to `build/libs/GhostDash-<version>.jar`.

## License

GhostDash is available under the [MIT License](LICENSE).
