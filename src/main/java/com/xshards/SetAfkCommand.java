package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class SetAfkCommand implements CommandExecutor {
    private final AfkManager afkManager;
    private final MessageManager messageManager;

    public SetAfkCommand(AfkManager afkManager, MessageManager messageManager) {
        this.afkManager = afkManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("command.only-player"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("xshards.admin")) {
            player.sendMessage(messageManager.get("command.no-permission"));
            return true;
        }

        // Set the AFK location to the player's current location
        afkManager.setAfkLocation(player);
        
        // Provide additional information about recommended AFK world setup
        player.sendMessage(messageManager.get("afk.set-admin-message"));
        player.sendMessage(messageManager.get("afk.set-info-1"));
        player.sendMessage(messageManager.get("afk.set-info-2"));

        return true; // Indicate the command was successful
    }
}