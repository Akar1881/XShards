package com.xshards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class ShopManager {
    private final Xshards plugin;
    private final Map<Integer, ShopItem> shopItems;
    private final DatabaseManager databaseManager;

    public ShopManager(Xshards plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        this.databaseManager = plugin.getDatabaseManager();
        loadShopData();
    }

    public void addItemToShop(int slot, ItemStack item, double price) {
        ItemStack shopItem = item.clone();
        ItemMeta meta = shopItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Price: " + ChatColor.LIGHT_PURPLE + price + "$ " + ChatColor.WHITE + "Shards");
            meta.setLore(lore);
            shopItem.setItemMeta(meta);
        }
        shopItems.put(slot, new ShopItem(item.clone(), price));
        saveShopData();
    }

    public void editItemPrice(int slot, double newPrice) {
        ShopItem existingItem = shopItems.get(slot);
        if (existingItem != null) {
            shopItems.put(slot, new ShopItem(existingItem.getItem(), newPrice));
            saveShopData();
        }
    }

    public void removeItemFromShop(int slot) {
        shopItems.remove(slot);
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM shop_items WHERE slot = ?")) {
            
            stmt.setInt(1, slot);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove shop item: " + e.getMessage());
        }
    }

    public ShopItem getItemInShop(int slot) {
        return shopItems.get(slot);
    }

    public void loadShopData() {
        shopItems.clear();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT slot, item_data, price, display_name, lore FROM shop_items");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int slot = rs.getInt("slot");
                byte[] itemData = rs.getBytes("item_data");
                double price = rs.getDouble("price");
                String displayName = rs.getString("display_name");
                String loreStr = rs.getString("lore");
                
                ItemStack item = DatabaseManager.deserializeItemStack(itemData);
                if (item != null) {
                    ShopItem shopItem = new ShopItem(item, price);
                    if (displayName != null && !displayName.isEmpty()) {
                        shopItem.setCustomDisplayName(displayName);
                    }
                    if (loreStr != null && !loreStr.isEmpty()) {
                        shopItem.setCustomLore(java.util.Arrays.asList(loreStr.split("\n")));
                    }
                    shopItems.put(slot, shopItem);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load shop data: " + e.getMessage());
        }
    }

    public void saveShopData() {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            String sql = databaseManager.getStorageType().equals("mysql")
                ? "INSERT INTO shop_items (slot, item_data, price, display_name, lore) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE item_data = VALUES(item_data), price = VALUES(price), display_name = VALUES(display_name), lore = VALUES(lore)"
                : "INSERT OR REPLACE INTO shop_items (slot, item_data, price, display_name, lore) VALUES (?, ?, ?, ?, ?)";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
                    ShopItem item = entry.getValue();
                    stmt.setInt(1, entry.getKey());
                    stmt.setBytes(2, DatabaseManager.serializeItemStack(item.getItem()));
                    stmt.setDouble(3, item.getPrice());
                    stmt.setString(4, item.getCustomDisplayName());
                    
                    if (item.getCustomLore() != null && !item.getCustomLore().isEmpty()) {
                        stmt.setString(5, String.join("\n", item.getCustomLore()));
                    } else {
                        stmt.setString(5, null);
                    }
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getShopTitle() {
        String shopTitle = plugin.getConfig().getString("store.title", "&6Shard Shop");
        return ChatColor.translateAlternateColorCodes('&', shopTitle);
    }

    public void openShopGUI(Player player) {
        int size = plugin.getConfig().getInt("store.size", 54);
        size = Math.min(54, Math.max(9, (size / 9) * 9));
        
        String shopTitle = getShopTitle();
        
        org.bukkit.inventory.Inventory shopInventory = Bukkit.createInventory(null, size, shopTitle);
        
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            int slot = entry.getKey();
            if (slot < size) {
                ShopItem shopItem = entry.getValue();
                ItemStack displayItem = shopItem.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                
                if (meta != null) {
                    // Apply custom display name from database if set
                    if (shopItem.getCustomDisplayName() != null && !shopItem.getCustomDisplayName().isEmpty()) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', shopItem.getCustomDisplayName()));
                    }
                    
                    // Apply custom lore from database if set
                    List<String> lore = new ArrayList<>();
                    if (shopItem.getCustomLore() != null && !shopItem.getCustomLore().isEmpty()) {
                        for (String loreLine : shopItem.getCustomLore()) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', loreLine.replace("{price}", String.valueOf(shopItem.getPrice()))));
                        }
                    } else {
                        // Use default lore from config
                        List<String> defaultLore = plugin.getConfig().getStringList("store.default-lore");
                        if (defaultLore != null && !defaultLore.isEmpty()) {
                            for (String loreLine : defaultLore) {
                                lore.add(ChatColor.translateAlternateColorCodes('&', loreLine.replace("{price}", String.valueOf(shopItem.getPrice()))));
                            }
                        } else {
                            // Fallback to default format
                            lore.add(ChatColor.WHITE + "Price: " + ChatColor.LIGHT_PURPLE + shopItem.getPrice() + "$ " + ChatColor.WHITE + "Shards");
                        }
                    }
                    
                    if (!lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                    displayItem.setItemMeta(meta);
                }
                shopInventory.setItem(slot, displayItem);
            }
        }
        player.openInventory(shopInventory);
    }

    public void setItemDisplayName(int slot, String displayName) {
        ShopItem item = shopItems.get(slot);
        if (item != null) {
            item.setCustomDisplayName(displayName);
            saveShopData();
        }
    }

    public void setItemLore(int slot, List<String> lore) {
        ShopItem item = shopItems.get(slot);
        if (item != null) {
            item.setCustomLore(lore);
            saveShopData();
        }
    }
}