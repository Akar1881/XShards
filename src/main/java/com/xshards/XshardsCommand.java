package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XshardsCommand implements CommandExecutor {
    private final Xshards plugin;
    private final MessageManager messageManager;

    public XshardsCommand(Xshards plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("xshards.admin")) {
                    sender.sendMessage(messageManager.get("command.no-permission"));
                    return true;
                }
                plugin.reloadPlugin(); // Use the reloadPlugin method which handles database reconnection
                sender.sendMessage(messageManager.get("xshards.reloaded"));
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(messageManager.get("xshards.unknown-command"));
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messageManager.get("xshards.help.header"));
        
        // General Commands
        sender.sendMessage(messageManager.get("xshards.help.general-title"));
        sender.sendMessage(messageManager.get("xshards.help.general-shards"));
        sender.sendMessage(messageManager.get("xshards.help.general-store"));
        sender.sendMessage(messageManager.get("xshards.help.general-afk"));
        sender.sendMessage(messageManager.get("xshards.help.general-quitafk"));

        // Shop Commands
        sender.sendMessage(messageManager.get("xshards.help.shop-title"));
        sender.sendMessage(messageManager.get("xshards.help.shop-edit"));
        sender.sendMessage(messageManager.get("xshards.help.shop-add"));
        sender.sendMessage(messageManager.get("xshards.help.shop-remove"));

        // Admin Commands
        if (sender.hasPermission("xshards.admin")) {
            sender.sendMessage(messageManager.get("xshards.help.admin-title"));
            sender.sendMessage(messageManager.get("xshards.help.admin-setafk"));
            sender.sendMessage(messageManager.get("xshards.help.admin-afkremove"));
            sender.sendMessage(messageManager.get("xshards.help.admin-reload"));
            sender.sendMessage(messageManager.get("xshards.help.admin-give"));
        }

        // Earning Methods Info
        sender.sendMessage(messageManager.get("xshards.help.earning-title"));
        sender.sendMessage(messageManager.get("xshards.help.earning-playtime"));
        sender.sendMessage(messageManager.get("xshards.help.earning-pvp"));
        sender.sendMessage(messageManager.get("xshards.help.earning-afk"));
        
        // Storage Info
        sender.sendMessage(messageManager.get("xshards.help.storage-title"));
        sender.sendMessage(messageManager.get("xshards.help.storage-current").replace("{type}", plugin.getDatabaseManager().getStorageType().toUpperCase()));
        sender.sendMessage(messageManager.get("xshards.help.storage-configure"));

        sender.sendMessage(messageManager.get("xshards.help.footer"));
    }
}