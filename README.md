# Punish 🛡️ — Ultimate Multi-Platform Moderation Ecosystem

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=java" alt="Java 21" />
  <img src="https://img.shields.io/badge/Minecraft-Spigot%20%7C%20Paper%20%7C%20Folia%20%7C%20Bungee%20%7C%20Velocity-green?logo=minecraft" alt="Minecraft Platforms" />
  <img src="https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven" alt="Maven Build" />
  <img src="https://img.shields.io/badge/Status-Beta--Testing-red" alt="Status Beta" />
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License" />
</p>

**Punish** is an industry-grade, lightweight, and completely customizable moderation and player-punishment ecosystem. Built from the ground up for high-capacity Minecraft networks, Punish runs natively on single Spigot/Paper/Purpur servers, regional-threaded Folia instances, and proxy networks like BungeeCord and Velocity—all bundled inside a single unified `.jar` file.

---

## 📢 🧪 Beta Testing Notice
> ⚠️ **Punish is currently in its active Beta Testing phase!** While highly optimized and fully operational, we strongly recommend thorough staging before deploying on massive production environments. If you encounter any bugs, crashes, or inconsistencies, **please file a detailed bug report** in our issues tracker to help us build a flawless release!

---

## ✨ Outstanding Features & Architecture

* 📡 **Unified Multi-Platform Support**: A single hybrid `.jar` file that runs natively under Spigot, PaperMC, Folia, BungeeCord, and Velocity, providing cross-server synchronization and consistent features.
* 🪜 **Customizable Punishment Ladders (Template System)**: Define punishment scaling ladders inside `templates.yml`. Repeat offenses automatically scale up durations and severity (e.g. 1st offense = 7d ban, 2nd = 14d, 3rd = permanent).
* 🖥️ **Accidental-Proof Moderation GUI**: Type `/punish <player>` to open a clean, non-flickering, modular GUI. Navigate duration selection, custom reasons, and confirm or cancel via an informational confirmation screen.
* 🔒 **Staff Abuse Protection & Rollbacks**: Prevent moderators from punishing higher-ranking staff via flexible permission weight settings. If staff abuse occurs, instantly revert all punishments issued by that staff member using a single command: `/punish rollback <staff_name>`.
* 🏛️ **Fully Restrictive Freezing & Jails**: Freeze players in place or teleport them to jail coordinates. Frozen/jailed players are restricted from block breaks, movement, inventory clicks, drops, picks, damage, and executing un-whitelisted commands.
* 🗄️ **High-Performance Asynchronous Database Layer**: Supports SQLite (default), MySQL, and MariaDB. All queries run asynchronously to keep your server ticking stably at a constant 20 TPS.
* 🗺️ **Dynamic Multi-Language System**: Supports seamless real-time translation using standard language abbreviations (EN, ID, CN, ES, RU) and dynamic file fallback on disk (e.g., `messages_en.yml`).
* 🎨 **Platform-Agnostic Broadcasts & Private Messages**: Customize private notification lines, global broadcasts, ActionBars, Titles, BossBars, particles, and sound effects for each individual punishment type.
* 💬 **Smart Auto-Completion**: Fully supports smart tab-completions for online usernames, preset reasons, and default duration formats.

---

## 🛠️ Commands & Permissions Directory

Punish features a completely decoupled permission directory. All nodes can be modified dynamically inside `permissions.yml` without re-compiling the code.

| Command | Description | Default Permission |
| :--- | :--- | :--- |
| `/ban <player> <reason> <duration>` | Ban a player permanently or temporarily. | `punish.ban` |
| `/kick <player> <reason>` | Kick an online player from the server. | `punish.kick` |
| `/mute <player> <reason> <duration>` | Mute a player from chat and commands. | `punish.mute` |
| `/freeze <player> <reason> <duration>` | Freeze a player's movement and actions. | `punish.freeze` |
| `/jail <player> <reason> <duration>` | Transport a player to the jail spawn zone. | `punish.jail` |
| `/warn <player> <reason>` | Issue a permanent warning to a player. | `punish.warn` |
| `/unban <player>` | Pardon a banned player account. | `punish.unban` |
| `/unmute <player>` | Restore chat privileges to a muted player. | `punish.unmute` |
| `/unfreeze <player>` | Melt and release a frozen player. | `punish.unfreeze` |
| `/unjail <player>` | Release a player back to the world spawn. | `punish.unjail` |
| `/clearwarn <player>` | Purge all active warnings for a player. | `punish.clearwarn` |
| `/delwarn <player> <warn-id>` | Delete a specific warning entry from history. | `punish.delwarn` |
| `/check <player>` | Inspect a player's active punishments. | `punish.check` |
| `/history <player>` | Open a player's punishment history ledger. | `punish.history` |
| `/punish <player>` | Open the Moderation GUI for a player. | `punish.gui` |
| `/punish reload` | Reload all configurations dynamically. | `punish.reload` |
| `/punish rollback <staff>` | Revert all punishments placed by a staff member. | `punish.admin` |
| `/setjail` | Configure the jail spawn location coordinate. | `punish.setjail` |

---

## 📄 License
This project is licensed under the **MIT License**. View the [LICENSE](LICENSE) file for more details.
