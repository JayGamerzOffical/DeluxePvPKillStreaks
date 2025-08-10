package org.JayGamerz;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KillStreakExpansion extends PlaceholderExpansion {

    private final KillStreakPlugin plugin;

    public KillStreakExpansion(KillStreakPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "killstreak";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JAY GAMERZ";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // %killstreak_current%
        if (params.equals("current")) {
            return String.valueOf(plugin.getKillStreak(player));
        }

        // %killstreak_player_best%
        if (params.equals("player_best")) {
            return String.valueOf(plugin.getBestKillStreak(player));
        }

        // %killstreak_top_player%
        if (params.equals("top_player")) {
            return plugin.getTopPlayer();
        }

        // %killstreak_top_streak%
        if (params.equals("top_streak")) {
            return String.valueOf(plugin.getTopStreak());
        }

        return null;
    }
}
