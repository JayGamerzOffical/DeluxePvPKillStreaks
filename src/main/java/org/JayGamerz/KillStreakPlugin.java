
package org.JayGamerz;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class KillStreakPlugin extends JavaPlugin implements Listener {
    private Map<UUID, Integer> killStreaks = new HashMap();
    private File configFile;
    private FileConfiguration config;

    public KillStreakPlugin() {
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("killstreak").setExecutor(new KillStreakCommand(this));
    }

    public void onDisable() {
        this.killStreaks.clear();
    }

    public void loadConfig() {
        this.configFile = new File(this.getDataFolder(), "config.yml");
        if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void saveConfigFile() {
        try {
            this.config.save(this.configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getPrefix() {
        return this.config.getString("prefix", "&7[KillStreak] ");
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer != null) {
            UUID killerID = killer.getUniqueId();
            UUID deadID = dead.getUniqueId();
            if (this.killStreaks.containsKey(deadID)) {
                int brokenStreak = (Integer)this.killStreaks.remove(deadID);
                if (brokenStreak >= 2) {
                    String breakMessage = this.config.getString("streak_broken_message", "&c%player%'s kill streak of %streak% has been broken!");
                    this.broadcastMessage(breakMessage.replace("%player%", dead.getName()).replace("%streak%", String.valueOf(brokenStreak)));
                }
            }

            this.killStreaks.put(killerID, (Integer)this.killStreaks.getOrDefault(killerID, 0) + 1);
            int newStreak = (Integer)this.killStreaks.get(killerID);
            if (this.config.getConfigurationSection("streak_messages") != null) {
                for(String key : this.config.getConfigurationSection("streak_messages").getKeys(false)) {
                    int milestone = Integer.parseInt(key);
                    if (newStreak == milestone) {
                        String message = this.config.getString("streak_messages." + key, "&6%player% reached a %streak% kill streak!");
                        this.broadcastMessage(message.replace("%player%", killer.getName()).replace("%streak%", key));
                        break;
                    }
                }
            }

        }
    }

    private void broadcastMessage(String message) {
        String var10000 = this.getPrefix();
        Bukkit.broadcastMessage(ColorManager.colorize(var10000 + message));
    }

    public void resetStreak(Player player) {
        this.killStreaks.remove(player.getUniqueId());
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        this.loadConfig();
    }
}
