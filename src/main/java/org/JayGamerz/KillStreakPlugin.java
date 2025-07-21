
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
import org.JayGamerz.database.DatabaseManager;

public class KillStreakPlugin extends JavaPlugin implements Listener {
    private Map<UUID, Integer> killStreaks = new HashMap<>();
    private Map<UUID, Integer> bestKillStreaks = new HashMap<>();
    private File configFile;
    private FileConfiguration config;
    private RewardManager rewardManager;
    private RewardConfigGUI rewardGUI;
    private KillStreakExpansion placeholderExpansion;
    private DatabaseManager databaseManager;
    private int leaderboardTaskId = -1;

    public KillStreakPlugin() {
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        
        // Initialize database
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.databaseManager.loadBestKillStreaks();
        
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
        // Schedule leaderboard update task
        int interval = this.getConfig().getInt("leaderboard_update_interval", 30);
        leaderboardTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::updateLeaderboards, interval * 20, interval * 20);
    }

    public void onDisable() {
        this.killStreaks.clear();
        // Don't clear bestKillStreaks as they're now persistent in database
        
        // Close database connection
        if (this.databaseManager != null) {
            this.databaseManager.closeConnection();
        }
        
        // Unregister PlaceholderAPI expansion
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
        }
        // Cancel leaderboard update task
        if (leaderboardTaskId != -1) {
            Bukkit.getScheduler().cancelTask(leaderboardTaskId);
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
            
            // Update best killstreak if current streak is higher
            int currentBest = this.bestKillStreaks.getOrDefault(killerID, 0);
            if (newStreak > currentBest) {
                this.bestKillStreaks.put(killerID, newStreak);
                // Save to database
                this.databaseManager.updateBestKillStreak(killerID, killer.getName(), newStreak);
            }
            
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

    // Show leaderboard using DecentHolograms
    public void showLeaderboard(Player player, boolean allTime) {
        org.bukkit.Location loc = player.getLocation().add(0, 2, 0);
        String holoName = allTime ? "killstreak_alltime" : "killstreak_current";
        org.bukkit.plugin.Plugin dh = org.bukkit.Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (dh == null) {
            player.sendMessage(colorize("&cDecentHolograms plugin not found!"));
            return;
        }
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(colorize("&e&lTop 10 " + (allTime ? "All-Time" : "Current") + " Killstreaks"));
        java.util.List<java.util.Map.Entry<UUID, Integer>> entries;
        if (allTime) {
            entries = new java.util.ArrayList<>(bestKillStreaks.entrySet());
        } else {
            entries = new java.util.ArrayList<>(killStreaks.entrySet());
        }
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        int rank = 1;
        for (java.util.Map.Entry<UUID, Integer> entry : entries) {
            if (rank > 10) break;
            org.bukkit.OfflinePlayer p = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            lines.add(colorize("&6" + rank + ". &e" + p.getName() + " &7- &b" + entry.getValue()));
            rank++;
        }
        try {
            Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            java.lang.reflect.Method createHolo = dhApi.getMethod("createHologram", String.class, org.bukkit.Location.class, java.util.List.class);
            createHolo.invoke(null, holoName + player.getUniqueId(), loc, lines);
        } catch (Exception e) {
            player.sendMessage(colorize("&cFailed to create leaderboard hologram: " + e.getMessage()));
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
        // Note: We don't reset the best killstreak as it's meant to be persistent
        // If you want to reset the best killstreak too, you would need a separate method
    }

    public void reloadPluginConfig() {
        this.reloadConfig();
        this.loadConfig();
    }
    
    // Methods for PlaceholderAPI expansion
    public int getKillStreak(Player player) {
        return this.killStreaks.getOrDefault(player.getUniqueId(), 0);
    }
    
    public int getBestKillStreak(Player player) {
        UUID playerUUID = player.getUniqueId();
        // First check in-memory cache
        if (this.bestKillStreaks.containsKey(playerUUID)) {
            return this.bestKillStreaks.get(playerUUID);
        }
        
        // If not in cache, load from database
        int dbBestStreak = this.databaseManager.getBestKillStreak(playerUUID);
        if (dbBestStreak > 0) {
            this.bestKillStreaks.put(playerUUID, dbBestStreak);
        }
        
        return dbBestStreak;
    }
    
    // Method to set best killstreak (used by database manager)
    public void setBestKillStreak(UUID playerUUID, int bestStreak) {
        this.bestKillStreaks.put(playerUUID, bestStreak);
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
    
    // Getter for the database manager
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }
    // Update all leaderboards (current and all-time) for all online players
    for (Player player : Bukkit.getOnlinePlayers()) {
        showLeaderboard(player, false);
        showLeaderboard(player, true);
    }
}
