package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderboardCommand implements CommandExecutor {
    private final LeaderboardManager leaderboardManager;
    private final MessageManager messageManager;

    public LeaderboardCommand(LeaderboardManager leaderboardManager, MessageManager messageManager) {
        this.leaderboardManager = leaderboardManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("command.only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("xshards.leaderboard")) {
            player.sendMessage(messageManager.get("command.no-permission"));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        leaderboardManager.openLeaderboard(player, page);
        return true;
    }
}
