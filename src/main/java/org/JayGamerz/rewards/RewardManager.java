package org.JayGamerz.rewards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.JayGamerz.KillStreakPlugin;

import java.util.List;
import java.util.Random;

public class RewardManager {
    
    private final KillStreakPlugin plugin;
    private final Random random;
    
    public RewardManager(KillStreakPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    public void processKillRewards(Player killer, int killStreak) {
        // Process chance-based rewards
        processChanceRewards(killer);
        
        // Process guaranteed rewards
        processGuaranteedRewards(killer, killStreak);
    }
    
    private void processChanceRewards(Player player) {
        if (!plugin.getConfig().getBoolean("chance_rewards.enabled", true)) {
            return;
        }
        
        var rewardsSection = plugin.getConfig().getConfigurationSection("chance_rewards.rewards");
        if (rewardsSection == null) return;
        
        for (String rewardKey : rewardsSection.getKeys(false)) {
            double chance = plugin.getConfig().getDouble("chance_rewards.rewards." + rewardKey + ".chance", 0.0);
            
            if (random.nextDouble() * 100.0 <= chance) {
                executeReward(player, "chance_rewards.rewards." + rewardKey);
            }
        }
    }
    
    private void processGuaranteedRewards(Player player, int killStreak) {
        if (!plugin.getConfig().getBoolean("guaranteed_rewards.enabled", true)) {
            return;
        }
        
        String rewardKey = String.valueOf(killStreak);
        String path = "guaranteed_rewards.rewards." + rewardKey;
        
        if (plugin.getConfig().contains(path)) {
            executeReward(player, path);
        }
    }
    
    private void executeReward(Player player, String rewardPath) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to execute reward for null or offline player");
            return;
        }
        
        List<String> commands = plugin.getConfig().getStringList(rewardPath + ".commands");
        String message = plugin.getConfig().getString(rewardPath + ".message", "");
        
        // Execute commands with safety checks
        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }
            
            // Basic command validation - prevent dangerous commands
            String lowerCommand = command.toLowerCase();
            if (lowerCommand.contains("stop") || lowerCommand.contains("shutdown") || 
                lowerCommand.contains("reload") || lowerCommand.contains("restart")) {
                plugin.getLogger().warning("Blocked potentially dangerous reward command: " + command);
                continue;
            }
            
            String processedCommand = command.replace("%player%", player.getName());
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute reward command '" + processedCommand + "': " + e.getMessage());
            }
        }
        
        // Send message
        if (!message.isEmpty()) {
            String processedMessage = message.replace("%player%", player.getName());
            player.sendMessage(plugin.colorize(plugin.getPrefix() + processedMessage));
        }
    }
}
