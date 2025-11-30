package com.xshards;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class LeaderboardListener implements Listener {
    private final LeaderboardManager leaderboardManager;

    public LeaderboardListener(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Shards Leaderboard")) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        String displayName = meta.getDisplayName();

        if (displayName.equals("§cPrevious Page")) {
            int currentPage = extractPageNumber(event.getView().getTitle());
            leaderboardManager.openLeaderboard(player, currentPage - 1);
        } else if (displayName.equals("§aNext Page")) {
            int currentPage = extractPageNumber(event.getView().getTitle());
            leaderboardManager.openLeaderboard(player, currentPage + 1);
        }
    }

    private int extractPageNumber(String title) {
        try {
            String[] parts = title.split(" ");
            return Integer.parseInt(parts[parts.length - 2]);
        } catch (Exception e) {
            return 1;
        }
    }
}
