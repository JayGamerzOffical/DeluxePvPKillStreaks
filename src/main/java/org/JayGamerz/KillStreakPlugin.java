
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
    
    // Constants for better maintainability
    private static final int TICKS_PER_SECOND = 20;
    private static final int MIN_LEADERBOARD_INTERVAL = 10; // seconds
    private static final int DEFAULT_LEADERBOARD_INTERVAL = 30; // seconds
    private static final int MIN_STREAK_FOR_ANNOUNCEMENT = 2;
    
    private Map<UUID, Integer> killStreaks = new HashMap<>();
    private Map<UUID, Integer> bestKillStreaks = new HashMap<>();
    private Map<String, org.bukkit.Location> serverLeaderboards = new HashMap<>(); // Track server-wide leaderboards by name and location
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
        this.validateConfig();
        
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
        int interval = this.getConfig().getInt("leaderboard_update_interval", DEFAULT_LEADERBOARD_INTERVAL);
        leaderboardTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this, 
            this::updateLeaderboards, 
            interval * TICKS_PER_SECOND, 
            interval * TICKS_PER_SECOND
        );
    }

    public void onDisable() {
        this.killStreaks.clear();
        // Don't clear bestKillStreaks as they're now persistent in database
        
        // Cleanup any remaining holograms
        cleanupAllHolograms();
        
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
    
    /**
     * Cleanup all server leaderboard holograms
     */
    private void cleanupAllHolograms() {
        if (serverLeaderboards.isEmpty()) {
            return;
        }
        
        try {
            org.bukkit.plugin.Plugin dh = org.bukkit.Bukkit.getPluginManager().getPlugin("DecentHolograms");
            if (dh != null) {
                Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                java.lang.reflect.Method removeHolo = dhApi.getMethod("removeHologram", String.class);
                
                for (String holoName : serverLeaderboards.keySet()) {
                    try {
                        removeHolo.invoke(null, holoName);
                    } catch (Exception e) {
                        getLogger().warning("Failed to cleanup hologram " + holoName + ": " + e.getMessage());
                    }
                }
                getLogger().info("Cleaned up " + serverLeaderboards.size() + " server leaderboard holograms");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to cleanup holograms: " + e.getMessage());
        } finally {
            serverLeaderboards.clear();
        }
    }

    public void loadConfig() {
        this.configFile = new File(this.getDataFolder(), "config.yml");
        if (!this.configFile.exists()) {
            this.saveResource("config.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }
    
    /**
     * Validates configuration values and sets defaults for invalid ones
     */
    private void validateConfig() {
        boolean configChanged = false;
        
        // Validate leaderboard update interval
        int interval = getConfig().getInt("leaderboard_update_interval", DEFAULT_LEADERBOARD_INTERVAL);
        if (interval < MIN_LEADERBOARD_INTERVAL) {
            getLogger().warning("leaderboard_update_interval too low (" + interval + "s), setting to minimum " + MIN_LEADERBOARD_INTERVAL + "s");
            getConfig().set("leaderboard_update_interval", MIN_LEADERBOARD_INTERVAL);
            configChanged = true;
        }
        
        // Validate announcement interval
        int everyNKills = getConfig().getInt("streak_announcement.every_n_kills", 5);
        if (everyNKills < 1) {
            getLogger().warning("streak_announcement.every_n_kills must be at least 1, setting to 5");
            getConfig().set("streak_announcement.every_n_kills", 5);
            configChanged = true;
        }
        
        // Save config if changes were made
        if (configChanged) {
            saveConfigFile();
            getLogger().info("Configuration auto-corrected and saved");
        }
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
                if (brokenStreak >= MIN_STREAK_FOR_ANNOUNCEMENT) {
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

    // Show leaderboard using DecentHolograms with persistent display
    public void showLeaderboard(Player player, boolean allTime) {
        org.bukkit.plugin.Plugin dh = org.bukkit.Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (dh == null) {
            player.sendMessage(colorize("&cDecentHolograms plugin not found!"));
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        String holoName = (allTime ? "killstreak_alltime_" : "killstreak_current_") + playerUUID;
        
        try {
            Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            java.lang.reflect.Method getHologram = dhApi.getMethod("getHologram", String.class);
            java.lang.reflect.Method createHolo = dhApi.getMethod("createHologram", String.class, org.bukkit.Location.class, java.util.List.class);
            java.lang.reflect.Method updateHolo = dhApi.getMethod("setHologramLines", Object.class, java.util.List.class);
            
            // Prepare hologram content
            java.util.List<String> lines = generateLeaderboardLines(allTime);
            
            // Check if hologram already exists
            Object existingHolo = getHologram.invoke(null, holoName);
            
            if (existingHolo != null) {
                // Update existing hologram content
                updateHolo.invoke(null, existingHolo, lines);
            } else {
                // Create new hologram at player's location (admin can move it later)
                org.bukkit.Location loc = player.getLocation().add(0, 2, 0);
                createHolo.invoke(null, holoName, loc, lines);
                serverLeaderboards.put(holoName, loc);
                player.sendMessage(colorize("&aLeaderboard hologram created! Use &e/killstreak removeboards &ato remove all leaderboards."));
            }
            
        } catch (Exception e) {
            player.sendMessage(colorize("&cFailed to create/update leaderboard hologram: " + e.getMessage()));
            getLogger().warning("Hologram operation failed: " + e.getMessage());
        }
    }
    
    /**
     * Generate leaderboard lines for hologram
     */
    private java.util.List<String> generateLeaderboardLines(boolean allTime) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(colorize("&e&lTop 10 " + (allTime ? "All-Time" : "Current") + " Killstreaks"));
        lines.add(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        java.util.List<java.util.Map.Entry<UUID, Integer>> entries;
        if (allTime) {
            entries = new java.util.ArrayList<>(bestKillStreaks.entrySet());
        } else {
            entries = new java.util.ArrayList<>(killStreaks.entrySet());
        }
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        int rank = 1;
        boolean hasEntries = false;
        
        for (java.util.Map.Entry<UUID, Integer> entry : entries) {
            if (rank > 10) break;
            org.bukkit.OfflinePlayer p = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey());
            String playerName = p.getName() != null ? p.getName() : "Unknown";
            lines.add(colorize("&6" + rank + ". &e" + playerName + " &7- &b" + entry.getValue()));
            rank++;
            hasEntries = true;
        }
        
        if (!hasEntries) {
            lines.add(colorize("&7No " + (allTime ? "records" : "active streaks") + " found"));
        }
        
        lines.add(colorize("&7&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        return lines;
    }
    
    /**
     * Remove all server leaderboard holograms
     */
    public void removeAllLeaderboards() {
        if (serverLeaderboards.isEmpty()) {
            return;
        }
        
        try {
            org.bukkit.plugin.Plugin dh = org.bukkit.Bukkit.getPluginManager().getPlugin("DecentHolograms");
            if (dh != null) {
                Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                java.lang.reflect.Method removeHolo = dhApi.getMethod("removeHologram", String.class);
                
                int removed = 0;
                for (String holoName : serverLeaderboards.keySet()) {
                    try {
                        removeHolo.invoke(null, holoName);
                        removed++;
                    } catch (Exception e) {
                        getLogger().warning("Failed to remove hologram " + holoName + ": " + e.getMessage());
                    }
                }
                serverLeaderboards.clear();
                getLogger().info("Removed " + removed + " leaderboard holograms");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to remove leaderboard holograms: " + e.getMessage());
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
    
    // Method to update existing server leaderboard holograms
    private void updateLeaderboards() {
        // Only update if there are server leaderboards to update
        if (serverLeaderboards.isEmpty()) {
            return;
        }
        
        try {
            org.bukkit.plugin.Plugin dh = org.bukkit.Bukkit.getPluginManager().getPlugin("DecentHolograms");
            if (dh == null) {
                return;
            }
            
            Class<?> dhApi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            java.lang.reflect.Method getHologram = dhApi.getMethod("getHologram", String.class);
            java.lang.reflect.Method updateHolo = dhApi.getMethod("setHologramLines", Object.class, java.util.List.class);
            
            int updated = 0;
            for (String holoName : serverLeaderboards.keySet()) {
                try {
                    Object hologram = getHologram.invoke(null, holoName);
                    if (hologram != null) {
                        // Determine if this is an all-time or current leaderboard
                        boolean isAllTime = holoName.contains("_alltime_");
                        java.util.List<String> newLines = generateLeaderboardLines(isAllTime);
                        updateHolo.invoke(null, hologram, newLines);
                        updated++;
                    } else {
                        // Hologram no longer exists, remove from tracking
                        serverLeaderboards.remove(holoName);
                    }
                } catch (Exception e) {
                    getLogger().warning("Failed to update hologram " + holoName + ": " + e.getMessage());
                }
            }
            
            if (updated > 0) {
                getLogger().fine("Updated " + updated + " leaderboard holograms");
            }
            
        } catch (Exception e) {
            getLogger().warning("Failed to update leaderboard holograms: " + e.getMessage());
        }
    }
}
