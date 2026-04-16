package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {
    private final Xshards plugin;
    private String storageType;
    private String host, database, username, password;
    private int port;
    private String sqliteFile;
    private boolean connected = false;
    private boolean driversLoaded = false;
    private boolean connectionLogged = false;

    public DatabaseManager(Xshards plugin) {
        this.plugin = plugin;
        loadConfig();
        loadDrivers();
        // Verify we can connect at least once and create tables.
        try (Connection testConn = openNewConnection()) {
            connected = true;
            if (!connectionLogged) {
                plugin.getLogger().info("Successfully connected to " + storageType.toUpperCase() + " database");
                connectionLogged = true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
        createTables();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        storageType = config.getString("storage.type", "sqlite").toLowerCase();

        if (storageType.equals("mysql")) {
            host = config.getString("storage.mysql.host", "localhost");
            port = config.getInt("storage.mysql.port", 3306);
            database = config.getString("storage.mysql.database", "xshards");
            username = config.getString("storage.mysql.user", "root");
            password = config.getString("storage.mysql.password", "password");
        } else {
            sqliteFile = config.getString("storage.sqlite.file", "plugins/Xshards/storage/xshards.db");
        }
    }

    private void loadDrivers() {
        try {
            if (storageType.equals("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }
            driversLoaded = true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("JDBC driver not found for " + storageType + ": " + e.getMessage());
        }
    }

    /**
     * Open a brand-new database connection. Each call returns a fresh
     * connection that the caller MUST close (typically via try-with-resources).
     * This avoids the "statement pointer closed" / "database has been closed"
     * errors caused by sharing one connection across threads.
     */
    private Connection openNewConnection() throws SQLException {
        if (!driversLoaded) {
            loadDrivers();
            if (!driversLoaded) {
                throw new SQLException("JDBC driver not loaded for " + storageType);
            }
        }

        Connection conn;
        if (storageType.equals("mysql")) {
            conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8",
                username, password);
        } else {
            File dbFile = new File(sqliteFile);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            conn = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
            try (Statement statement = conn.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
                // Improve concurrent read/write behaviour on SQLite.
                statement.execute("PRAGMA journal_mode = WAL;");
                statement.execute("PRAGMA busy_timeout = 5000;");
            } catch (SQLException ignored) {
                // PRAGMAs are best-effort; connection is still usable.
            }
        }
        return conn;
    }

    public void close() {
        // Nothing to keep open; connections are per-call now.
        connected = false;
    }

    /**
     * Get a database connection. Returns a NEW connection each time.
     * Callers MUST close the returned connection (use try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return openNewConnection();
    }

    public void createTables() {
        try {
            if (storageType.equals("mysql")) {
                createMySQLTables();
            } else {
                createSQLiteTables();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMySQLTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "shards INT NOT NULL DEFAULT 0" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            statement.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INT PRIMARY KEY, " +
                    "item_data MEDIUMBLOB NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "display_name VARCHAR(256), " +
                    "lore LONGTEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            statement.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            statement.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            statement.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "is_afk BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "start_time BIGINT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        }
    }

    private void createSQLiteTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "shards INTEGER NOT NULL DEFAULT 0" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INTEGER PRIMARY KEY, " +
                    "item_data BLOB NOT NULL, " +
                    "price REAL NOT NULL, " +
                    "display_name TEXT, " +
                    "lore TEXT" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "is_afk INTEGER NOT NULL DEFAULT 0, " +
                    "start_time INTEGER" +
                    ");");
        }
    }

    public static byte[] serializeItemStack(ItemStack item) {
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream dataOutput = new java.io.ObjectOutputStream(outputStream);
            dataOutput.writeObject(item.serialize());
            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to serialize ItemStack: " + e.getMessage());
            return new byte[0];
        }
    }

    public static ItemStack deserializeItemStack(byte[] data) {
        try {
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(data);
            java.io.ObjectInputStream dataInput = new java.io.ObjectInputStream(inputStream);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> serializedItem = (java.util.Map<String, Object>) dataInput.readObject();
            dataInput.close();
            return ItemStack.deserialize(serializedItem);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to deserialize ItemStack: " + e.getMessage());
            return null;
        }
    }

    public String getStorageType() {
        return storageType;
    }

    public boolean isConnected() {
        return connected;
    }
}
