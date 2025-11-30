package com.xshards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class DailyRewardListener implements Listener {
    private final DailyRewardManager dailyRewardManager;

    public DailyRewardListener(DailyRewardManager dailyRewardManager) {
        this.dailyRewardManager = dailyRewardManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check and give daily reward
        dailyRewardManager.checkAndGiveReward(player);
    }
}
