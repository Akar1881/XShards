package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardManager {
    private final Xshards plugin;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final int itemsPerPage = 7;

    public LeaderboardManager(Xshards plugin, DatabaseManager databaseManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.messageManager = messageManager;
    }

    public void openLeaderboard(Player player, int page) {
        List<LeaderboardEntry> entries = getTopPlayers();

        int totalPages = (entries.size() + itemsPerPage - 1) / itemsPerPage;
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        Inventory inv = Bukkit.createInventory(null, 27, "Shards Leaderboard - Page " + page);

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            LeaderboardEntry entry = entries.get(i);
            ItemStack skull = createPlayerSkull(entry.getPlayerName(), entry.getRank(), entry.getShards());
            inv.addItem(skull);
        }

        // Add navigation items
        if (page > 1) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevItem.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§cPrevious Page");
                prevItem.setItemMeta(prevMeta);
            }
            inv.setItem(18, prevItem);
        }

        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§ePages: " + page + "/" + totalPages);
            pageInfo.setItemMeta(pageMeta);
        }
        inv.setItem(22, pageInfo);

        if (page < totalPages) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextItem.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§aNext Page");
                nextItem.setItemMeta(nextMeta);
            }
            inv.setItem(26, nextItem);
        }

        player.openInventory(inv);
    }

    private ItemStack createPlayerSkull(String playerName, int rank, int shards) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        
        if (skullMeta != null) {
            skullMeta.setOwner(playerName);
            skullMeta.setDisplayName("§6[#" + rank + "] " + playerName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§eShards: §6" + shards);
            skullMeta.setLore(lore);
            
            skull.setItemMeta(skullMeta);
        }
        
        return skull;
    }

    private List<LeaderboardEntry> getTopPlayers() {
        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid, player_name, shards FROM player_shards ORDER BY shards DESC LIMIT 100")) {

            ResultSet rs = stmt.executeQuery();
            int rank = 1;

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String playerName = rs.getString("player_name");
                int shards = rs.getInt("shards");

                entries.add(new LeaderboardEntry(uuid, playerName, shards, rank));
                rank++;
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Error loading leaderboard: " + e.getMessage());
        }

        return entries;
    }

    public static class LeaderboardEntry {
        private final String uuid;
        private final String playerName;
        private final int shards;
        private final int rank;

        public LeaderboardEntry(String uuid, String playerName, int shards, int rank) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.shards = shards;
            this.rank = rank;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getShards() {
            return shards;
        }

        public int getRank() {
            return rank;
        }
    }
}
