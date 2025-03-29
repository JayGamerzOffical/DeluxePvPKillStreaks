# KillStreaks by Jay Gamerz™ – Customizable PvP Kill Streak Plugin

🚀 **KillStreaks** is a powerful and fully customizable Paper plugin that **broadcasts kill streak milestones** and **alerts players when a streak is broken**. Perfect for PvP servers, this plugin brings excitement and engagement to every battle!

---

## 📌 Features

✅ **Custom Kill Streak Messages** – Define unique messages for different streak levels.  
✅ **Kill Streak Break Alerts** – Notify players when a high kill streak is broken.  
✅ **Permission-Based Commands** – Control who can reload configs or reset streaks.  
✅ **Fully Configurable** – Customize all messages, colors, and formats.  
✅ **Lightweight & Lag-Free** – Optimized for smooth performance.  
✅ **Supports Paper & Spigot 1.20+** – Works with modern Minecraft versions.

---

## 📥 Installation

1. **Download the plugin** from [Modrinth](https://modrinth.com/) or [Spigot](https://www.spigotmc.org/).
2. **Move the file** into your server’s `/plugins` folder.
3. **Restart or reload** the server.
4. **Edit `config.yml`** to customize the plugin settings.
5. **Use `/killstreak reload`** to apply changes without restarting.

---

## 🎮 Commands & Permissions

| Command | Description | Permission |
|---------|------------|------------|
| `/killstreak reload` | Reloads the plugin configuration. | `killstreak.reload` |
| `/killstreak reset <player>` | Resets a player’s kill streak. | `killstreak.reset` |

📢 **Note:** Players **do not need** any permission to earn kill streaks or trigger broadcasts.

---

## ⚙ Configuration (`config.yml`)

You can fully customize the plugin using the `config.yml` file:

```yaml
# KillStreaks by Jay Gamerz™ – Fully Customizable Kill Streak Plugin

# Prefix for all messages
prefix: "&6[KillStreak] "

# Messages for kill streak milestones
streak_messages:
  "3": "&e%player% is on a 3 kill streak!"  
  "5": "&c%player% is unstoppable with a 5 kill streak!"  
  "10": "&4%player% is DOMINATING with a 10 kill streak!"  

# Message when a streak is broken
streak_broken_message: "&c%player%'s kill streak of %streak% was broken!"

# General messages
messages:
  no_permission: "&cYou don't have permission!"  
  player_only: "&cOnly players can use this command!"  
  player_not_found: "&cPlayer %player% not found!"  
  config_reloaded: "&aConfig reloaded successfully!"  
  streak_reset: "&aReset %player%'s kill streak!"  
  usage: "&eUsage: /killstreak reload | /killstreak reset <player>"  
