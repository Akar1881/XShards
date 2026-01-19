package com.xshards;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class XshardsPlaceholder extends PlaceholderExpansion {
    private final Xshards plugin;
    private final ShardManager shardManager;

    public XshardsPlaceholder(Xshards plugin, ShardManager shardManager) {
        this.plugin = plugin;
        this.shardManager = shardManager;
        register(); // Register the placeholders
    }

    @Override
    public String getIdentifier() {
        return "xshards"; // The identifier used in the placeholders
    }

    @Override
    public String getAuthor() {
        return "Akar1881"; // Your name or the author's name
    }

    @Override
    public String getVersion() {
        return "1.0"; // Version of your placeholder expansion
    }

    @Override
    public boolean persist() {
        return true; // This is important for some PAPI versions
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return ""; // Return empty string if the player is null
        }

        // Placeholder to get the player's shards
        if (identifier.equals("playershards")) {
            return String.valueOf(shardManager.getShards(player));
        }

        // Leaderboard placeholders: %xshards_top_name_1%, %xshards_top_shards_1%
        if (identifier.startsWith("top_name_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(9));
                return plugin.getLeaderboardManager().getPlayerAtRank(rank);
            } catch (NumberFormatException e) {
                return "";
            }
        }

        if (identifier.startsWith("top_shards_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(11));
                return String.valueOf(plugin.getLeaderboardManager().getShardsAtRank(rank));
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        return null; // Placeholder not found
    }
}