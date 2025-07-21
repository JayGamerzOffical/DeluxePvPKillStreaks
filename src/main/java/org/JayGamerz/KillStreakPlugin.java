
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
import org.JayGamerz.rewards.RewardManager;
import org.JayGamerz.gui.RewardConfigGUI;

public class KillStreakPlugin extends JavaPlugin implements Listener {
    private Map<UUID, Integer> killStreaks = new HashMap<>();
    private File configFile;
    private FileConfiguration config;
    private RewardManager rewardManager;
    private RewardConfigGUI rewardGUI;
    private KillStreakExpansion placeholderExpansion;

    public KillStreakPlugin() {
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        
        // Initialize managers
        this.rewardManager = new RewardManager(this);
        this.rewardGUI = new RewardConfigGUI(this);
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(this.rewardGUI, this);
        
        // Register commands
        this.getCommand("killstreak").setExecutor(new KillStreakCommand(this));
        this.getCommand("killstreakgui").setExecutor(new KillStreakCommand(this));
        
        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion = new KillStreakExpansion(this);
            this.placeholderExpansion.register();
            getLogger().info("PlaceholderAPI expansion registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
        
        getLogger().info("KillStreak Plugin v3.0 enabled!");
    }

    public void onDisable() {
        this.killStreaks.clear();
        
        // Unregister PlaceholderAPI expansion
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
        }
        
        getLogger().info("KillStreak Plugin disabled!");
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
            
            // Handle broken streak
            if (this.killStreaks.containsKey(deadID)) {
                int brokenStreak = this.killStreaks.remove(deadID);
                if (brokenStreak >= 2) {
                    String breakMessage = this.config.getString("streak_broken_message", "&c%player%'s kill streak of %streak% has been broken!");
                    this.broadcastMessage(breakMessage.replace("%player%", dead.getName()).replace("%streak%", String.valueOf(brokenStreak)));
                }
            }

            // Increment killer's streak
            this.killStreaks.put(killerID, this.killStreaks.getOrDefault(killerID, 0) + 1);
            int newStreak = this.killStreaks.get(killerID);
            
            // Process rewards
            this.rewardManager.processKillRewards(killer, newStreak);
            
            // Handle streak announcements
            handleStreakAnnouncement(killer, newStreak);
        }
    }
    
    private void handleStreakAnnouncement(Player killer, int newStreak) {
        // Check if new announcement system is enabled
        if (this.config.getBoolean("streak_announcement.enabled", true)) {
            int everyNKills = this.config.getInt("streak_announcement.every_n_kills", 5);
            
            if (newStreak % everyNKills == 0) {
                String message = this.config.getString("streak_announcement.message", "&e%player% is on a %streak% kill streak!");
                this.broadcastMessage(message.replace("%player%", killer.getName()).replace("%streak%", String.valueOf(newStreak)));
            }
        } else {
            // Use legacy system for backwards compatibility
            if (this.config.getConfigurationSection("streak_messages") != null) {
                for (String key : this.config.getConfigurationSection("streak_messages").getKeys(false)) {
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
        String formattedMessage = ColorManager.colorize(this.getPrefix() + message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formattedMessage);
        }
    }

    public void resetStreak(Player player) {
        this.killStreaks.remove(player.getUniqueId());
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        this.loadConfig();
    }
    
    // Methods for PlaceholderAPI expansion
    public int getKillStreak(Player player) {
        return this.killStreaks.getOrDefault(player.getUniqueId(), 0);
    }
    
    public String getTopPlayer() {
        String topPlayer = "None";
        int topStreak = 0;
        
        for (Map.Entry<UUID, Integer> entry : this.killStreaks.entrySet()) {
            if (entry.getValue() > topStreak) {
                topStreak = entry.getValue();
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    topPlayer = player.getName();
                }
            }
        }
        
        return topPlayer;
    }
    
    public int getTopStreak() {
        int topStreak = 0;
        
        for (int streak : this.killStreaks.values()) {
            if (streak > topStreak) {
                topStreak = streak;
            }
        }
        
        return topStreak;
    }
    
    // Method for ColorManager access
    public String colorize(String message) {
        return ColorManager.colorize(message);
    }
    
    // Getter for the reward GUI
    public RewardConfigGUI getRewardGUI() {
        return this.rewardGUI;
    }
}
