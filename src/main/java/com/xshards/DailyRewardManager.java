package com.xshards;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DailyRewardManager {
    private final Xshards plugin;
    private final DatabaseManager databaseManager;
    private final MessageManager messageManager;
    private final Map<UUID, StreakData> streakCache;

    public DailyRewardManager(Xshards plugin, DatabaseManager databaseManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.messageManager = messageManager;
        this.streakCache = new HashMap<>();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                    ? "CREATE TABLE IF NOT EXISTS daily_rewards (uuid VARCHAR(36) PRIMARY KEY, last_reward_date DATE, streak_days INT DEFAULT 0, multiplier DOUBLE DEFAULT 1.0, stage VARCHAR(20) DEFAULT 'easy')"
                    : "CREATE TABLE IF NOT EXISTS daily_rewards (uuid TEXT PRIMARY KEY, last_reward_date TEXT, streak_days INTEGER DEFAULT 0, multiplier REAL DEFAULT 1.0, stage TEXT DEFAULT 'easy')";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize daily rewards table: " + e.getMessage());
        }
    }

    public void checkAndGiveReward(Player player) {
        if (!plugin.getConfig().getBoolean("daily-rewards.enabled", true)) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        LocalDate today = LocalDate.now();

        // Get or load streak data
        StreakData streakData = getStreakData(playerUUID);
        LocalDate lastClaimDate = streakData.getLastLoginDate();

        // Check if player already claimed today
        if (lastClaimDate != null && lastClaimDate.equals(today)) {
            return; // Already claimed today
        }

        // Check if streak should be reset (missed a day)
        if (lastClaimDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(lastClaimDate, today);
            if (daysBetween > 1) {
                // Streak broken - reset to 1 day and 1.0x multiplier
                streakData.setStreakDays(1);
                streakData.setMultiplier(1.0);
                streakData.setStage("easy");
                player.sendMessage(messageManager.get("daily-reward.streak-reset"));
            } else if (daysBetween == 1) {
                // Continue streak
                streakData.setStreakDays(streakData.getStreakDays() + 1);
                updateMultiplier(streakData);
            }
        } else {
            // First time claiming
            streakData.setStreakDays(1);
            streakData.setMultiplier(1.0);
            streakData.setStage("easy");
        }

        // Update last login date
        streakData.setLastLoginDate(today);

        // Calculate reward with multiplier
        int baseAmount = plugin.getConfig().getInt("daily-rewards.amount", 10);
        int finalAmount = (int) (baseAmount * streakData.getMultiplier());
        
        // Give reward
        plugin.getShardManager().addShards(player, finalAmount);

        // Send messages
        String rewardMessage = messageManager.get("daily-reward.received")
                .replace("{amount}", String.valueOf(finalAmount));
        player.sendMessage(rewardMessage);

        if (plugin.getConfig().getBoolean("daily-rewards.streak.enabled", true)) {
            String streakMessage = messageManager.get("daily-reward.streak-active")
                    .replace("{days}", String.valueOf(streakData.getStreakDays()))
                    .replace("{multiplier}", String.format("%.1f", streakData.getMultiplier()))
                    .replace("{stage}", streakData.getStage());
            player.sendMessage(streakMessage);
        }

        // Save streak data
        saveStreakData(playerUUID, streakData);
    }

    private void updateMultiplier(StreakData streakData) {
        if (!plugin.getConfig().getBoolean("daily-rewards.streak.enabled", true)) {
            return;
        }

        double increment = plugin.getConfig().getDouble("daily-rewards.streak.increment", 0.1);
        String activeStage = plugin.getConfig().getString("daily-rewards.streak.active_stage", "easy");
        
        int interval;
        if (activeStage.equalsIgnoreCase("hard")) {
            interval = plugin.getConfig().getInt("daily-rewards.streak.hard.interval", 7);
        } else if (activeStage.equalsIgnoreCase("mid")) {
            interval = plugin.getConfig().getInt("daily-rewards.streak.mid.interval", 3);
        } else {
            interval = plugin.getConfig().getInt("daily-rewards.streak.easy.interval", 1);
        }

        int streakDays = streakData.getStreakDays();
        double currentMultiplier = streakData.getMultiplier();

        // Check if we should advance multiplier based on active stage interval
        if (streakDays % interval == 0 && streakDays > 0) {
            currentMultiplier += increment;
            streakData.setStage(activeStage);
        }

        streakData.setMultiplier(Math.max(1.0, currentMultiplier)); // Never go below 1.0x
    }

    private StreakData getStreakData(UUID playerUUID) {
        // Check cache first
        if (streakCache.containsKey(playerUUID)) {
            return streakCache.get(playerUUID);
        }

        // Load from database
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT last_reward_date, streak_days, multiplier, stage FROM daily_rewards WHERE uuid = ?")) {
            
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String dateStr = rs.getString("last_reward_date");
                int streakDays = rs.getInt("streak_days");
                double multiplier = rs.getDouble("multiplier");
                String stage = rs.getString("stage");
                
                LocalDate lastDate = dateStr != null ? LocalDate.parse(dateStr) : null;
                StreakData data = new StreakData(streakDays, lastDate, multiplier, stage);
                streakCache.put(playerUUID, data);
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting streak data: " + e.getMessage());
        }

        // Return new streak data if not found
        StreakData newData = new StreakData();
        streakCache.put(playerUUID, newData);
        return newData;
    }

    private void saveStreakData(UUID playerUUID, StreakData data) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql;
            if (databaseManager.getStorageType().equals("mysql")) {
                sql = "INSERT INTO daily_rewards (uuid, last_reward_date, streak_days, multiplier, stage) VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE last_reward_date = ?, streak_days = ?, multiplier = ?, stage = ?";
            } else {
                sql = "INSERT OR REPLACE INTO daily_rewards (uuid, last_reward_date, streak_days, multiplier, stage) VALUES (?, ?, ?, ?, ?)";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, data.getLastLoginDate() != null ? data.getLastLoginDate().toString() : null);
                stmt.setInt(3, data.getStreakDays());
                stmt.setDouble(4, data.getMultiplier());
                stmt.setString(5, data.getStage());
                
                if (!databaseManager.getStorageType().equals("sqlite")) {
                    stmt.setString(6, data.getLastLoginDate() != null ? data.getLastLoginDate().toString() : null);
                    stmt.setInt(7, data.getStreakDays());
                    stmt.setDouble(8, data.getMultiplier());
                    stmt.setString(9, data.getStage());
                }
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving streak data: " + e.getMessage());
        }
    }
}
