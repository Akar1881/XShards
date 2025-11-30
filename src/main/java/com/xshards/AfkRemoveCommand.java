package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class AfkRemoveCommand implements CommandExecutor {
    private final AfkManager afkManager;
    private final MessageManager messageManager;

    public AfkRemoveCommand(AfkManager afkManager, MessageManager messageManager) {
        this.afkManager = afkManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("afk.remove-only-player"));
            return true;
        }

        Player player = (Player) sender;

        // Check if the player has the required permission
        if (!player.hasPermission("xshards.admin")) {
            player.sendMessage(messageManager.get("command.no-permission"));
            return true;
        }

        // Remove the AFK location
        afkManager.removeAfkLocation();
        player.sendMessage(messageManager.get("afk.remove-success"));

        return true;
    }
}