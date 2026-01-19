package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShardCommand implements CommandExecutor {
    private final ShardManager shardManager;
    private final MessageManager messageManager;

    public ShardCommand(ShardManager shardManager, MessageManager messageManager) {
        this.shardManager = shardManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle console commands
        if (!(sender instanceof Player)) {
            // Console can only use the give command
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(messageManager.get("shards.player-not-found"));
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    if (args[0].equalsIgnoreCase("give")) {
                        shardManager.addShards(targetPlayer, amount);
                        sender.sendMessage(messageManager.get("shards.given").replace("{amount}", String.valueOf(amount)).replace("{player}", targetPlayer.getName()));
                        targetPlayer.sendMessage(messageManager.get("shards.received").replace("{amount}", String.valueOf(amount)).replace("{sender}", "Console"));
                    } else if (args[0].equalsIgnoreCase("remove")) {
                        shardManager.removeShards(targetPlayer, amount);
                        sender.sendMessage(messageManager.get("shards.removed").replace("{amount}", String.valueOf(amount)).replace("{player}", targetPlayer.getName()));
                        targetPlayer.sendMessage(messageManager.get("shards.taken").replace("{amount}", String.valueOf(amount)).replace("{sender}", "Console"));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(messageManager.get("shards.invalid-amount"));
                }
                return true;
            } else {
                sender.sendMessage(messageManager.get("shards.console-usage"));
                return true;
            }
        }

        // Handle player commands
        Player player = (Player) sender;

        // /shards command with no arguments: check the player's shard balance
        if (args.length == 0) {
            int playerShards = shardManager.getShards(player);
            player.sendMessage(messageManager.get("shards.balance").replace("{amount}", String.valueOf(playerShards)));
            return true;
        }

        // /shards give/remove <player> <amount>
        if (args.length == 3 && player.hasPermission("xshards.admin")) {
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage(messageManager.get("shards.player-not-found"));
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                if (args[0].equalsIgnoreCase("give")) {
                    shardManager.addShards(targetPlayer, amount);
                    player.sendMessage(messageManager.get("shards.given").replace("{amount}", String.valueOf(amount)).replace("{player}", targetPlayer.getName()));
                    targetPlayer.sendMessage(messageManager.get("shards.received").replace("{amount}", String.valueOf(amount)).replace("{sender}", player.getName()));
                } else if (args[0].equalsIgnoreCase("remove")) {
                    shardManager.removeShards(targetPlayer, amount);
                    player.sendMessage(messageManager.get("shards.removed").replace("{amount}", String.valueOf(amount)).replace("{player}", targetPlayer.getName()));
                    targetPlayer.sendMessage(messageManager.get("shards.taken").replace("{amount}", String.valueOf(amount)).replace("{sender}", player.getName()));
                } else {
                    player.sendMessage(messageManager.get("shards.usage"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(messageManager.get("shards.invalid-amount"));
            }
            return true;
        }

        player.sendMessage(messageManager.get("shards.usage"));
        return true;
    }
}