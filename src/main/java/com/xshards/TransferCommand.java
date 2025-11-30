package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferCommand implements CommandExecutor {
    private final ShardManager shardManager;
    private final MessageManager messageManager;

    public TransferCommand(ShardManager shardManager, MessageManager messageManager) {
        this.shardManager = shardManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("command.only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("xshards.transfer")) {
            player.sendMessage(messageManager.get("command.no-permission"));
            return true;
        }

        if (!Bukkit.getPluginManager().getPlugin("Xshards").getConfig().getBoolean("transfer.enabled", true)) {
            player.sendMessage(messageManager.get("transfer.disabled"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(messageManager.get("transfer.usage"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            player.sendMessage(messageManager.get("shards.player-not-found"));
            return true;
        }

        if (targetPlayer.equals(player)) {
            player.sendMessage(messageManager.get("transfer.cannot-self"));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(messageManager.get("transfer.invalid-amount"));
                return true;
            }

            int playerShards = shardManager.getShards(player);
            if (playerShards < amount) {
                player.sendMessage(messageManager.get("transfer.insufficient"));
                return true;
            }

            shardManager.initiateTransfer(player, targetPlayer, amount);
            player.sendMessage(messageManager.get("transfer.initiated").replace("{player}", targetPlayer.getName()).replace("{amount}", String.valueOf(amount)));

        } catch (NumberFormatException e) {
            player.sendMessage(messageManager.get("shards.invalid-amount"));
        }
        return true;
    }
}
