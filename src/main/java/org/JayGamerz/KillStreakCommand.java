
package org.JayGamerz;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillStreakCommand implements CommandExecutor {
    private final KillStreakPlugin plugin;

    public KillStreakCommand(KillStreakPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("killstreak.reload")) {
                    sender.sendMessage(this.getMessage("no_permission"));
                    return true;
                }

                this.plugin.reloadPluginConfig();
                sender.sendMessage(this.getMessage("config_reloaded"));
                return true;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(this.getMessage("player_only"));
                return true;
            }

            if (!sender.hasPermission("killstreak.reset")) {
                sender.sendMessage(this.getMessage("no_permission"));
                return true;
            }

            Player target = this.plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(this.getMessage("player_not_found").replace("%player%", args[1]));
                return true;
            }

            this.plugin.resetStreak(target);
            sender.sendMessage(this.getMessage("streak_reset").replace("%player%", target.getName()));
            return true;
        }

        sender.sendMessage(this.getMessage("usage"));
        return true;
    }

    private String getMessage(String key) {
        String var10000 = this.plugin.getPrefix();
        return var10000 + this.plugin.getConfig().getString("messages." + key, "&cMessage not found in config!").replace("&", "§");
    }
}
