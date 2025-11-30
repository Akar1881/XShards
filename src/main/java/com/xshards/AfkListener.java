package com.xshards;

import com.xshards.utils.ActionBarUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Bukkit;

public class AfkListener implements Listener {
    private final AfkManager afkManager;
    private final MessageManager messageManager;

    public AfkListener(AfkManager afkManager, MessageManager messageManager) {
        this.afkManager = afkManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is marked as AFK (could happen after server restart)
        if (afkManager.isAfk(player)) {
            // Schedule a task to run after the player has fully joined
            // This ensures auth plugins have time to process the player
            Bukkit.getScheduler().runTaskLater(afkManager.getPlugin(), () -> {
                // Remove AFK status automatically
                afkManager.removeAfkData(player);
                player.sendMessage(messageManager.get("afk.removed-on-join"));
            }, 20L); // 1 second delay (20 ticks)
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Allow only /quitafk command while AFK
        if (afkManager.isAfk(player) && !command.startsWith("/quitafk")) {
            player.sendMessage(messageManager.get("afk.commands-blocked"));
            event.setCancelled(true); // Cancel other commands
        }
    }
    
    // We're removing the movement restriction for AFK players
    // This allows players to move freely while in AFK mode
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Cancel interactions while in AFK mode
        if (afkManager.isAfk(player)) {
            event.setCancelled(true);
            ActionBarUtil.sendActionBar(player, messageManager.get("afk.interaction-blocked"));
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Prevent damage to AFK players
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (afkManager.isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Prevent AFK players from dealing damage
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (afkManager.isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (afkManager.isAfk(player)) {
            afkManager.removeAfkData(player); // Clean up AFK data on quit
        }
    }
}