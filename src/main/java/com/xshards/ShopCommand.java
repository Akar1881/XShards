package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final ShopManager shopManager;
    private final MessageManager messageManager;

    public ShopCommand(ShopManager shopManager, MessageManager messageManager) {
        this.shopManager = shopManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.get("shop.command-only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            shopManager.openShopGUI(player);
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("edit") && player.hasPermission("xshards.admin")) {
                if (args.length != 3) {
                    player.sendMessage(messageManager.get("shop.edit-usage"));
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    double price = Double.parseDouble(args[2]);
                    
                    if (shopManager.getItemInShop(slot) == null) {
                        player.sendMessage(messageManager.get("shop.no-item-in-slot").replace("{slot}", String.valueOf(slot)));
                        return true;
                    }
                    
                    shopManager.editItemPrice(slot, price);
                    player.sendMessage(messageManager.get("shop.edit-success").replace("{slot}", String.valueOf(slot)).replace("{price}", String.valueOf(price)));
                } catch (NumberFormatException e) {
                    player.sendMessage(messageManager.get("shop.invalid-number"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("add") && player.hasPermission("xshards.admin")) {
                if (args.length != 3) {
                    player.sendMessage(messageManager.get("shop.add-usage"));
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    double price = Double.parseDouble(args[2]);

                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        player.sendMessage(messageManager.get("shop.add-no-item"));
                        return true;
                    }

                    shopManager.addItemToShop(slot, item, price);
                    player.sendMessage(messageManager.get("shop.add-success").replace("{slot}", String.valueOf(slot)).replace("{price}", String.valueOf(price)));
                } catch (NumberFormatException e) {
                    player.sendMessage(messageManager.get("shop.invalid-number"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("remove") && player.hasPermission("xshards.admin")) {
                if (args.length != 2) {
                    player.sendMessage(messageManager.get("shop.remove-usage"));
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    shopManager.removeItemFromShop(slot);
                    player.sendMessage(messageManager.get("shop.remove-success").replace("{slot}", String.valueOf(slot)));
                } catch (NumberFormatException e) {
                    player.sendMessage(messageManager.get("shop.invalid-number"));
                }
                return true;
            }
        }

        player.sendMessage(messageManager.get("shop.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("xshards.admin")) {
                completions.addAll(Arrays.asList("edit", "add", "remove"));
            }
        } else if (args.length == 2) {
            if (sender.hasPermission("xshards.admin")) {
                // Suggest slots 0-53 for the second argument
                for (int i = 0; i < 54; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 3) {
            if (sender.hasPermission("xshards.admin") && 
                (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("add"))) {
                // Suggest some common prices
                completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
            }
        }
        
        return completions;
    }
}