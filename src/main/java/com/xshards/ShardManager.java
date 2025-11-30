package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ShardManager {
    private final Xshards plugin;
    private final Map<UUID, ShardData> playerData;
    private final Map<UUID, ShopItem> pendingPurchases;
    private final Map<UUID, TransferListener.TransferData> pendingTransfers;
    private final DatabaseManager databaseManager;

    public ShardManager(Xshards plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.pendingPurchases = new HashMap<>();
        this.pendingTransfers = new HashMap<>();
        this.databaseManager = plugin.getDatabaseManager();
        loadAllPlayerData();
    }

    // Add shards to a player's account
    public void addShards(Player player, int amount) {
        UUID playerUUID = player.getUniqueId();
        ShardData data = playerData.getOrDefault(playerUUID, new ShardData());
        data.addShards(amount);
        playerData.put(playerUUID, data);
        player.sendMessage("You earned " + amount + " shards!");
        savePlayerData(player); // Save player's data immediately after updating
    }

    // Get the number of shards for a player
    public int getShards(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // First check the in-memory cache
        if (playerData.containsKey(playerUUID)) {
            return playerData.get(playerUUID).getShards();
        }
        
        // If not in cache, load from database
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT shards FROM player_shards WHERE uuid = ?")) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int shards = rs.getInt("shards");
                playerData.put(playerUUID, new ShardData(shards));
                return shards;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error getting player shards: " + e.getMessage());
        }
        
        // If not found in database, return 0
        playerData.put(playerUUID, new ShardData(0));
        return 0;
    }

    // Save the data for a specific player
    public void savePlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();
        int shards = playerData.getOrDefault(playerUUID, new ShardData()).getShards();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 databaseManager.getStorageType().equals("mysql") 
                     ? "INSERT INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), shards = VALUES(shards)"
                     : "INSERT OR REPLACE INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?)"
             )) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, shards);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + playerName + "!", e);
        }
    }

    // Load data for a specific player
    public void loadPlayerData(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT shards FROM player_shards WHERE uuid = ?")) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int shards = rs.getInt("shards");
                playerData.put(playerUUID, new ShardData(shards));
            } else {
                // Player not found in database, create new entry with 0 shards
                playerData.put(playerUUID, new ShardData(0));
                savePlayerData(player); // Save the new player data
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load player data for " + player.getName() + "!", e);
            // Default to 0 shards if there's an error
            playerData.put(playerUUID, new ShardData(0));
        }
    }

    // Save data for all players
    public void saveAllPlayerData() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                ? "INSERT INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), shards = VALUES(shards)"
                : "INSERT OR REPLACE INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?)";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<UUID, ShardData> entry : playerData.entrySet()) {
                    UUID playerUUID = entry.getKey();
                    Player player = Bukkit.getPlayer(playerUUID);
                    
                    if (player != null) {
                        stmt.setString(1, playerUUID.toString());
                        stmt.setString(2, player.getName());
                        stmt.setInt(3, entry.getValue().getShards());
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save all player data!", e);
        }
    }

    // Load data for all players (used at startup or if needed to load full data)
    public void loadAllPlayerData() {
        // Clear existing data
        playerData.clear();
        
        // Load data for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
        
        // We don't need to load offline players' data until they join
    }

    // Pending purchase methods
    public void setPendingPurchase(Player player, ShopItem item) {
        pendingPurchases.put(player.getUniqueId(), item);
    }

    public ShopItem getPendingPurchase(Player player) {
        return pendingPurchases.get(player.getUniqueId());
    }

    public void clearPendingPurchase(Player player) {
        pendingPurchases.remove(player.getUniqueId());
    }

    public void initiateTransfer(Player sender, Player recipient, int amount) {
        Bukkit.createInventory(null, 9, "Confirm Transfer");
        org.bukkit.inventory.Inventory confirmInv = Bukkit.createInventory(null, 9, "Confirm Transfer");

        ItemStack confirmItem = new ItemStack(org.bukkit.Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§aConfirm Transfer of " + amount + " shards to " + recipient.getName());
            confirmItem.setItemMeta(confirmMeta);
        }

        ItemStack cancelItem = new ItemStack(org.bukkit.Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cCancel Transfer");
            cancelItem.setItemMeta(cancelMeta);
        }

        confirmInv.setItem(3, confirmItem);
        confirmInv.setItem(5, cancelItem);

        sender.openInventory(confirmInv);
        pendingTransfers.put(sender.getUniqueId(), new TransferListener.TransferData(recipient.getUniqueId(), amount));
    }

    public TransferListener.TransferData getPendingTransfer(Player player) {
        return pendingTransfers.get(player.getUniqueId());
    }

    public void clearPendingTransfer(Player player) {
        pendingTransfers.remove(player.getUniqueId());
    }
}