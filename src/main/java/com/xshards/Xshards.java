package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Xshards extends JavaPlugin {
    private ShardManager shardManager;
    private ShopManager shopManager;
    private AfkManager afkManager;
    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private DailyRewardManager dailyRewardManager;
    private LeaderboardManager leaderboardManager;
    private TransferListener transferListener;

    @Override
    public void onEnable() {
        // Initialize ActionBarUtil
        com.xshards.utils.ActionBarUtil.initialize();
        
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Create storage directory if it doesn't exist
        File storageDir = new File(getDataFolder(), "storage");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Initialize database manager first
        databaseManager = new DatabaseManager(this);
        
        // Initialize managers
        shardManager = new ShardManager(this);
        shopManager = new ShopManager(this);
        afkManager = new AfkManager(this, messageManager);
        dailyRewardManager = new DailyRewardManager(this, databaseManager, messageManager);
        leaderboardManager = new LeaderboardManager(this, databaseManager, messageManager);
        transferListener = new TransferListener(shardManager, messageManager);

        // Register commands and listeners
        getCommand("shards").setExecutor(new ShardCommand(shardManager, messageManager));
        getCommand("store").setExecutor(new ShopCommand(shopManager, messageManager));
        getCommand("xshards").setExecutor(new XshardsCommand(this, messageManager));
        getCommand("afk").setExecutor(new AfkCommand(afkManager, messageManager));
        getCommand("setafk").setExecutor(new SetAfkCommand(afkManager, messageManager));
        getCommand("quitafk").setExecutor(new QuitAfkCommand(afkManager, messageManager));
        getCommand("afkremove").setExecutor(new AfkRemoveCommand(afkManager, messageManager));
        getCommand("transfer").setExecutor(new TransferCommand(shardManager, messageManager));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(leaderboardManager, messageManager));
        getServer().getPluginManager().registerEvents(new ShardListener(shardManager, this, messageManager), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, shardManager, messageManager), this);
        getServer().getPluginManager().registerEvents(new AfkListener(afkManager, messageManager), this);
        getServer().getPluginManager().registerEvents(new DailyRewardListener(dailyRewardManager), this);
        getServer().getPluginManager().registerEvents(new LeaderboardListener(leaderboardManager), this);
        getServer().getPluginManager().registerEvents(transferListener, this);

        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new XshardsPlaceholder(this, shardManager).register();
            getLogger().info("PlaceholderAPI detected. Shards placeholders registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
        }

        // Clear any lingering AFK data from previous server session
        try {
            databaseManager.getConnection().prepareStatement("DELETE FROM afk_status").executeUpdate();
            getLogger().info("AFK status data has been reset on server startup.");
        } catch (Exception e) {
            getLogger().warning("Failed to clear AFK status data: " + e.getMessage());
        }

        getLogger().info("Xshards v1.2.4 has been enabled!");
    }

    @Override
    public void onDisable() {
        // Check if managers were initialized properly
        if (afkManager != null) {
            // Remove all players from AFK mode
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (afkManager.isAfk(player)) {
                    afkManager.quitAfk(player);
                }
            }
        }

        // Save all data if managers were initialized
        if (shardManager != null) {
            shardManager.saveAllPlayerData();
        }
        
        if (shopManager != null) {
            shopManager.saveShopData();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("Xshards has been disabled.");
    }

    // No longer needed as we're not using YAML files

    public ShardManager getShardManager() {
        return this.shardManager;
    }

    public ShopManager getShopManager() {
        return this.shopManager;
    }

    public AfkManager getAfkManager() {
        return this.afkManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public DailyRewardManager getDailyRewardManager() {
        return this.dailyRewardManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return this.leaderboardManager;
    }

    public void reloadPlugin() {
        reloadConfig();
        messageManager.reload();
        
        // Reload database connection if storage type changed
        String currentStorageType = databaseManager.getStorageType();
        String configStorageType = getConfig().getString("storage.type", "sqlite");
        
        if (!currentStorageType.equals(configStorageType)) {
            getLogger().info("Storage type changed from " + currentStorageType + " to " + configStorageType + ". Reconnecting...");
            databaseManager.close();
            databaseManager = new DatabaseManager(this);
            
            // Reload all data
            shardManager.loadAllPlayerData();
            shopManager.loadShopData();
            afkManager.loadAfkData();
            afkManager.loadAfkLocation();
        }
    }
}