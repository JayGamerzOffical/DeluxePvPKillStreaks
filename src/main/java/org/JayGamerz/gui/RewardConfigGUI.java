package org.JayGamerz.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.JayGamerz.KillStreakPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardConfigGUI implements Listener {
    
    private final KillStreakPlugin plugin;
    private final Map<UUID, String> editingPlayers = new HashMap<>();
    
    public RewardConfigGUI(KillStreakPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void openMainGUI(Player player) {
        // For now, send a message about configuration
        player.sendMessage(plugin.colorize(plugin.getPrefix() + "&eReward Configuration GUI is under development!"));
        player.sendMessage(plugin.colorize(plugin.getPrefix() + "&7Please use the config.yml file to configure rewards."));
        player.sendMessage(plugin.colorize(plugin.getPrefix() + "&7Use /killstreak reload after making changes."));
        
        // Show some helpful information about the current configuration
        player.sendMessage(plugin.colorize(plugin.getPrefix() + "&6Current Configuration:"));
        player.sendMessage(plugin.colorize("&7- Announcements every: &e" + plugin.getConfig().getInt("streak_announcement.every_n_kills", 5) + " kills"));
        player.sendMessage(plugin.colorize("&7- Chance rewards enabled: &e" + plugin.getConfig().getBoolean("chance_rewards.enabled", true)));
        player.sendMessage(plugin.colorize("&7- Guaranteed rewards enabled: &e" + plugin.getConfig().getBoolean("guaranteed_rewards.enabled", true)));
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Placeholder for future GUI implementation
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            editingPlayers.remove(player.getUniqueId());
        }
    }
}
