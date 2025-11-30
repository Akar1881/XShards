package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.ArrayList;

public class ShopListener implements Listener {
    private final ShopManager shopManager;
    private final ShardManager shardManager;
    private final MessageManager messageManager;

    public ShopListener(ShopManager shopManager, ShardManager shardManager, MessageManager messageManager) {
        this.shopManager = shopManager;
        this.shardManager = shardManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Shard Shop")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                int slot = event.getSlot();
                ShopItem item = shopManager.getItemInShop(slot);
                if (item != null) {
                    if (shardManager.getShards(player) >= item.getPrice()) {
                        openConfirmationGUI(player, item);
                    } else {
                        player.sendMessage(messageManager.get("shop.insufficient-shards"));
                    }
                }
            }
        }
    }

    private void openConfirmationGUI(Player player, ShopItem item) {
        org.bukkit.inventory.Inventory confirmationGui = Bukkit.createInventory(null, 9, "Confirm Purchase");

        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm Purchase");
            confirmItem.setItemMeta(confirmMeta);
        }

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cCancel Purchase");
            cancelItem.setItemMeta(cancelMeta);
        }

        confirmationGui.setItem(3, confirmItem);
        confirmationGui.setItem(5, cancelItem);
        player.openInventory(confirmationGui);
        shardManager.setPendingPurchase(player, item);
    }

    @EventHandler
    public void onConfirmationClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Confirm Purchase")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ShopItem pendingItem = shardManager.getPendingPurchase(player);

            if (pendingItem != null && event.getCurrentItem() != null) {
                if (event.getCurrentItem().getType() == Material.GREEN_WOOL) {
                    double price = pendingItem.getPrice();
                    ItemStack purchasedItem = pendingItem.getItem().clone();
                    // Remove price lore before giving to player
                    ItemMeta meta = purchasedItem.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            lore.removeIf(line -> line.contains("Price:"));
                            meta.setLore(lore);
                            purchasedItem.setItemMeta(meta);
                        }
                    }
                    shardManager.addShards(player, -((int)price));
                    player.getInventory().addItem(purchasedItem);
                    player.sendMessage(messageManager.get("shop.purchase-success").replace("{item}", purchasedItem.getType().toString()).replace("{price}", String.valueOf(price)));
                } else if (event.getCurrentItem().getType() == Material.RED_WOOL) {
                    player.sendMessage(messageManager.get("shop.purchase-cancelled"));
                }
                shardManager.clearPendingPurchase(player);
                player.closeInventory();
            }
        }
    }
}