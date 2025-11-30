package com.xshards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShardListener implements Listener {
    private final ShardManager shardManager;
    private final Xshards plugin;
    private final MessageManager messageManager;
    private final Map<UUID, Map<UUID, Long>> lastKillTimestamps = new HashMap<>();
    private final Map<UUID, Long> lastEarnedTime = new HashMap<>();

    public ShardListener(ShardManager shardManager, Xshards plugin, MessageManager messageManager) {
        this.shardManager = shardManager;
        this.plugin = plugin;
        this.messageManager = messageManager;
        startShardEarningTask();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        shardManager.loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        shardManager.savePlayerData(player);
    }

    private void startShardEarningTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if playtime earning is enabled
                if (!plugin.getConfig().getBoolean("earning.playtime.enabled", true)) {
                    return;
                }

                long earnShardTime = plugin.getConfig().getLong("earning.playtime.interval", 3600000);
                int shards = plugin.getConfig().getInt("earning.playtime.amount", 3);

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    if (lastEarnedTime.containsKey(playerId)) {
                        long lastTime = lastEarnedTime.get(playerId);
                        if ((currentTime - lastTime) >= earnShardTime) {
                            shardManager.addShards(player, shards);
                            player.sendMessage(messageManager.get("shards.playtime-earned").replace("{amount}", String.valueOf(shards)));
                            lastEarnedTime.put(playerId, currentTime);
                        }
                    } else {
                        lastEarnedTime.put(playerId, currentTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200); // Runs every minute
    }

    @EventHandler
    public void onPlayerKill(EntityDeathEvent event) {
        // Check if kill earning is enabled
        if (!plugin.getConfig().getBoolean("earning.kills.enabled", true)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player killedPlayer = (Player) entity;
            Player killer = killedPlayer.getKiller();

            if (killer != null) {
                UUID killerUUID = killer.getUniqueId();
                UUID killedUUID = killedPlayer.getUniqueId();

                long currentTime = System.currentTimeMillis();
                Map<UUID, Long> killerKillTimestamps = lastKillTimestamps.getOrDefault(killerUUID, new HashMap<>());

                if (!killerKillTimestamps.containsKey(killedUUID) || 
                    (currentTime - killerKillTimestamps.get(killedUUID) >= 24 * 60 * 60 * 1000)) {
                    int shardsPerKill = plugin.getConfig().getInt("earning.kills.amount", 10);
                    shardManager.addShards(killer, shardsPerKill);
                    killer.sendMessage(messageManager.get("shards.kill-earned").replace("{amount}", String.valueOf(shardsPerKill)).replace("{player}", killedPlayer.getName()));

                    killerKillTimestamps.put(killedUUID, currentTime);
                    lastKillTimestamps.put(killerUUID, killerKillTimestamps);
                } else {
                    killer.sendMessage(messageManager.get("shards.kill-cooldown").replace("{player}", killedPlayer.getName()));
                }
            }
        }
    }
}