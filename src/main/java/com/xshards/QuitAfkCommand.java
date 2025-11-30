package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandExecutor;

public class QuitAfkCommand implements CommandExecutor {
    private final AfkManager afkManager;
    private final MessageManager messageManager;

    public QuitAfkCommand(AfkManager afkManager, MessageManager messageManager) {
        this.afkManager = afkManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("command.only-player"));
            return true;
        }

        Player player = (Player) sender;

        // Check if the player is currently in AFK mode
        if (!afkManager.isAfk(player)) {
            player.sendMessage(messageManager.get("afk.not-in-afk"));
            return true;
        }

        // Call the quit method from AfkManager
        afkManager.quitAfk(player);
        return true; // Return true to indicate successful command execution
    }
}