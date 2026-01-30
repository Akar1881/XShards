package com.xshards;

import com.xshards.utils.ActionBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AfkManager {
    private final Xshards plugin;
    private final Map<UUID, AfkData> afkData;
    private Location afkLocation;
    private final Map<UUID, Long> afkStartTime;
    private final DatabaseManager databaseManager;
    private final Map<UUID, Location> pendingAfkTeleports;
    private final Map<UUID, BukkitRunnable> afkCountdowns;
    private final Map<UUID, BossBar> afkBossBars;
    private final MessageManager messageManager;

    public AfkManager(Xshards plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.afkData = new HashMap<>();
        this.afkStartTime = new HashMap<>();
        this.databaseManager = plugin.getDatabaseManager();
        this.pendingAfkTeleports = new HashMap<>();
        this.afkCountdowns = new HashMap<>();
        this.afkBossBars = new HashMap<>();
        loadAfkLocation();
        loadAfkData();
    }

    private void savePlayerLocation(Player player) {
        UUID playerUUID = player.getUniqueId();
        Location loc = player.getLocation();
        
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                ? "INSERT INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)"
                : "INSERT OR REPLACE INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, loc.getWorld().getName());
                stmt.setDouble(3, loc.getX());
                stmt.setDouble(4, loc.getY());
                stmt.setDouble(5, loc.getZ());
                stmt.setFloat(6, loc.getYaw());
                stmt.setFloat(7, loc.getPitch());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player location: " + e.getMessage());
        }
    }

    private Location getStoredPlayerLocation(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM player_locations WHERE uuid = ?")) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                float yaw = rs.getFloat("yaw");
                float pitch = rs.getFloat("pitch");
                
                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get stored player location: " + e.getMessage());
        }
        
        return null;
    }

    public void loadAfkLocation() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT world, x, y, z FROM afk_location WHERE id = 1");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                
                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    afkLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
                    plugin.getLogger().info("Loaded AFK location from database.");
                } else {
                    afkLocation = null;
                    plugin.getLogger().warning("Could not load AFK location: world not found.");
                }
            } else {
                afkLocation = null;
                plugin.getLogger().info("No AFK location found in database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load AFK location: " + e.getMessage());
            afkLocation = null;
        }
    }

    public void removeAfkLocation() {
        afkLocation = null;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM afk_location WHERE id = 1")) {
            
            stmt.executeUpdate();
            plugin.getLogger().info("Removed AFK location from database.");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove AFK location: " + e.getMessage());
        }
    }

    public void saveAfkLocation() {
        if (afkLocation != null) {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = databaseManager.getStorageType().equals("mysql")
                    ? "INSERT INTO afk_location (id, world, x, y, z) VALUES (1, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z)"
                    : "INSERT OR REPLACE INTO afk_location (id, world, x, y, z) VALUES (1, ?, ?, ?, ?)";
                    
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, afkLocation.getWorld().getName());
                    stmt.setDouble(2, afkLocation.getX());
                    stmt.setDouble(3, afkLocation.getY());
                    stmt.setDouble(4, afkLocation.getZ());
                    stmt.executeUpdate();
                    plugin.getLogger().info("Saved AFK location to database.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save AFK location: " + e.getMessage());
            }
        }
    }

    public void setAfkLocation(Player player) {
        // Check if the world is Nether or End
        World world = player.getWorld();
        if (world.getEnvironment() == Environment.NETHER || world.getEnvironment() == Environment.THE_END) {
            player.sendMessage(messageManager.get("afk.invalid-world"));
            player.sendMessage(messageManager.get("afk.invalid-world-info"));
            return;
        }
        
        afkLocation = player.getLocation();
        saveAfkLocation();
        player.sendMessage(messageManager.get("afk.location-set"));
    }

    public void startAfkProcess(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Check if player is already in AFK mode
        if (isAfk(player)) {
            player.sendMessage(messageManager.get("afk.already-afk"));
            return;
        }
        
        // Check if AFK location is set
        if (afkLocation == null) {
            player.sendMessage(messageManager.get("afk.no-location"));
            return;
        }
        
        // Save player's current location
        savePlayerLocation(player);
        
        // Store initial location for movement check
        final Location initialLocation = player.getLocation().clone();
        pendingAfkTeleports.put(playerUUID, initialLocation);
        
        // Create boss bar for countdown
        BossBar bossBar = Bukkit.createBossBar(
            messageManager.get("afk.teleport-boss-bar"),
            BarColor.YELLOW,
            BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        afkBossBars.put(playerUUID, bossBar);
        
        // Start countdown
        player.sendMessage(messageManager.get("afk.startup-message"));
        
        BukkitRunnable countdown = new BukkitRunnable() {
            int seconds = 5;
            
            @Override
            public void run() {
                // Check if player moved
                if (hasPlayerMoved(player, initialLocation)) {
                    player.sendMessage(messageManager.get("afk.teleport-cancelled"));
                    cancelAfkProcess(player);
                    return;
                }
                
                // Update boss bar
                bossBar.setProgress(seconds / 5.0);
                
                if (seconds <= 0) {
                    // Teleport player to AFK location
                    completeAfkProcess(player);
                    cancel();
                } else {
                    ActionBarUtil.sendActionBar(player, messageManager.get("afk.teleport-countdown").replace("{seconds}", String.valueOf(seconds)));
                    seconds--;
                }
            }
        };
        
        countdown.runTaskTimer(plugin, 0, 20);
        afkCountdowns.put(playerUUID, countdown);
    }
    
    private void completeAfkProcess(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Clean up countdown resources
        if (afkBossBars.containsKey(playerUUID)) {
            afkBossBars.get(playerUUID).removeAll();
            afkBossBars.remove(playerUUID);
        }
        
        pendingAfkTeleports.remove(playerUUID);
        
        // Teleport player to AFK location
        player.teleport(afkLocation);
        player.sendMessage(messageManager.get("afk.entered-mode"));
        
        // Set AFK status
        afkData.put(playerUUID, new AfkData());
        afkStartTime.put(playerUUID, System.currentTimeMillis());
        
        // Save to database
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                ? "INSERT INTO afk_status (uuid, is_afk, start_time) VALUES (?, 1, ?) " +
                  "ON DUPLICATE KEY UPDATE is_afk = 1, start_time = VALUES(start_time)"
                : "INSERT OR REPLACE INTO afk_status (uuid, is_afk, start_time) VALUES (?, 1, ?)";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not set player AFK status: " + e.getMessage());
        }
        
        // Start shard earning
        startAfkShardEarning(player);
    }
    
    private void cancelAfkProcess(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Cancel countdown task
        if (afkCountdowns.containsKey(playerUUID)) {
            afkCountdowns.get(playerUUID).cancel();
            afkCountdowns.remove(playerUUID);
        }
        
        // Remove boss bar
        if (afkBossBars.containsKey(playerUUID)) {
            afkBossBars.get(playerUUID).removeAll();
            afkBossBars.remove(playerUUID);
        }
        
        pendingAfkTeleports.remove(playerUUID);
    }

    public Location getAfkLocation() {
        return afkLocation;
    }

    public Location getLastLocation(Player player) {
        Location storedLocation = getStoredPlayerLocation(player);
        return storedLocation != null ? storedLocation : player.getLocation();
    }

    public boolean isAfk(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // First check the in-memory cache
        if (afkData.containsKey(playerUUID)) {
            return true;
        }
        
        // If not in cache, check the database
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT is_afk FROM afk_status WHERE uuid = ? AND is_afk = 1")) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // If found in database, add to cache
                afkData.put(playerUUID, new AfkData());
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error checking AFK status: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean isPendingAfk(Player player) {
        return pendingAfkTeleports.containsKey(player.getUniqueId());
    }

    public void loadAfkData() {
        afkData.clear();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid, start_time FROM afk_status WHERE is_afk = 1");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                long startTime = rs.getLong("start_time");
                
                afkData.put(playerUUID, new AfkData());
                afkStartTime.put(playerUUID, startTime);
            }
            
            plugin.getLogger().info("Loaded AFK data for " + afkData.size() + " players.");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load AFK data: " + e.getMessage());
        }
    }

    public void removeAfkData(Player player) {
        UUID playerUUID = player.getUniqueId();
        afkData.remove(playerUUID);
        afkStartTime.remove(playerUUID);
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM afk_status WHERE uuid = ?")) {
            
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove AFK data: " + e.getMessage());
        }
    }

    public void quitAfk(Player player) {
        if (isAfk(player)) {
            Location lastLocation = getLastLocation(player);
            if (lastLocation != null) {
                player.teleport(lastLocation);
                player.sendMessage(messageManager.get("afk.exited-mode"));
            } else {
                // If we can't find the last location, don't teleport the player
                player.sendMessage(messageManager.get("afk.exited-mode-no-location"));
            }
            // Always remove AFK data regardless of teleport success
            removeAfkData(player);
        } else if (isPendingAfk(player)) {
            // Cancel pending AFK teleport
            cancelAfkProcess(player);
            player.sendMessage(messageManager.get("afk.teleport-cancelled-manual"));
        } else {
            player.sendMessage(messageManager.get("afk.not-in-afk"));
        }
    }

    private void startAfkShardEarning(Player player) {
        long afkEarnSeconds = plugin.getConfig().getLong("earning.afk.interval", 30);
        int afkAmount = plugin.getConfig().getInt("earning.afk.amount", 1);

        new BukkitRunnable() {
            int countdown = (int) afkEarnSeconds;

            @Override
            public void run() {
                if (isAfk(player) && player.isOnline()) {
                    // Use action bar instead of title for less intrusive display
                    ActionBarUtil.sendActionBar(player, messageManager.get("afk.earn-countdown").replace("{seconds}", String.valueOf(countdown)));

                    if (countdown <= 0) {
                        plugin.getShardManager().addShards(player, afkAmount);
                        if (plugin.getConfig().getBoolean("earning.afk.message-enabled", true)) {
                            player.sendMessage(messageManager.get("afk.earned").replace("{amount}", String.valueOf(afkAmount)));
                        }
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
                        countdown = (int) afkEarnSeconds;
                    } else {
                        countdown--;
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public boolean hasPlayerMoved(Player player, Location initialLocation) {
        Location currentLocation = player.getLocation();
        double distanceSquared = initialLocation.distanceSquared(currentLocation);
        // Allow very small movements (less than 0.1 blocks) to account for server-side adjustments
        return distanceSquared > 0.01;
    }

    public Xshards getPlugin() {
        return this.plugin;
    }
}