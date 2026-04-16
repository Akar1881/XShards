package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

public class ShopListener implements Listener {
    private static final String CONFIRM_TITLE = "Confirm Purchase";

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
        String title = event.getView().getTitle();

        // Confirmation GUI handling
        if (CONFIRM_TITLE.equals(title)) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            // Only react to clicks inside the confirmation top inventory.
            Inventory top = event.getView().getTopInventory();
            if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            ShopItem pendingItem = shardManager.getPendingPurchase(player);
            if (pendingItem == null) {
                player.closeInventory();
                return;
            }

            if (clicked.getType() == Material.GREEN_WOOL) {
                double price = pendingItem.getPrice();
                int priceInt = (int) Math.ceil(price);

                // Re-verify the player still has enough shards (defends against
                // race conditions and stale GUIs).
                if (shardManager.getShards(player) < priceInt) {
                    player.sendMessage(messageManager.get("shop.insufficient-shards"));
                    shardManager.clearPendingPurchase(player);
                    player.closeInventory();
                    return;
                }

                // Deduct shards FIRST so a failed item delivery cannot grant a free item.
                shardManager.removeShards(player, priceInt);

                ItemStack purchasedItem = pendingItem.getItem().clone();
                ItemMeta meta = purchasedItem.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        lore.removeIf(line -> line.contains("Price:"));
                        meta.setLore(lore);
                        purchasedItem.setItemMeta(meta);
                    }
                }

                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(purchasedItem);
                if (leftover != null && !leftover.isEmpty()) {
                    // Inventory was full; drop remainder at the player's feet so they
                    // never lose what they paid for.
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }

                player.sendMessage(messageManager.get("shop.purchase-success")
                    .replace("{item}", purchasedItem.getType().toString())
                    .replace("{price}", String.valueOf(price)));
            } else if (clicked.getType() == Material.RED_WOOL) {
                player.sendMessage(messageManager.get("shop.purchase-cancelled"));
            } else {
                return;
            }

            shardManager.clearPendingPurchase(player);
            player.closeInventory();
            return;
        }

        // Shop GUI handling
        if (title.equals(shopManager.getShopTitle())) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            // Only react to clicks in the shop's top inventory; ignore clicks
            // in the player's own inventory below.
            Inventory top = event.getView().getTopInventory();
            if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            int slot = event.getRawSlot();
            ShopItem item = shopManager.getItemInShop(slot);
            if (item == null) {
                return;
            }

            int priceInt = (int) Math.ceil(item.getPrice());
            if (shardManager.getShards(player) >= priceInt) {
                openConfirmationGUI(player, item);
            } else {
                player.sendMessage(messageManager.get("shop.insufficient-shards"));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (CONFIRM_TITLE.equals(event.getView().getTitle())
                && event.getPlayer() instanceof Player) {
            shardManager.clearPendingPurchase((Player) event.getPlayer());
        }
    }

    private void openConfirmationGUI(Player player, ShopItem item) {
        Inventory confirmationGui = Bukkit.createInventory(null, 9, CONFIRM_TITLE);

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

        // Track pending purchase BEFORE opening the inventory so the close
        // listener and click handler can rely on the state being set.
        shardManager.setPendingPurchase(player, item);
        player.openInventory(confirmationGui);
    }
}
