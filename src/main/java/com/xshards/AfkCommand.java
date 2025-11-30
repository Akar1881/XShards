package com.xshards;

import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class AfkCommand implements CommandExecutor {
    private final AfkManager afkManager;
    private final MessageManager messageManager;

    public AfkCommand(AfkManager afkManager, MessageManager messageManager) {
        this.afkManager = afkManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("command.only-player"));
            return true;
        }

        Player player = (Player) sender;

        // Check if player has permission
        if (!player.hasPermission("xshards.use")) {
            player.sendMessage(messageManager.get("command.no-permission"));
            return true;
        }

        // Check if AFK earning is enabled
        if (!afkManager.getPlugin().getConfig().getBoolean("earning.afk.enabled", true)) {
            player.sendMessage(messageManager.get("afk.disabled"));
            return true;
        }

        if (afkManager.isAfk(player)) {
            player.sendMessage(messageManager.get("afk.already-afk"));
            return true;
        }
        
        if (afkManager.isPendingAfk(player)) {
            player.sendMessage(messageManager.get("afk.already-pending"));
            return true;
        }

        // Start the AFK process with the improved system
        afkManager.startAfkProcess(player);
        
        return true;
    }
}