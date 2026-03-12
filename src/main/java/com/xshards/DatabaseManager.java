package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {
    private final Xshards plugin;
    private Connection connection;
    private String storageType;
    private String host, database, username, password;
    private int port;
    private String sqliteFile;
    private boolean connected = false;

    public DatabaseManager(Xshards plugin) {
        this.plugin = plugin;
        loadConfig();
        connect();
        createTables();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        storageType = config.getString("storage.type", "sqlite").toLowerCase();
        
        // Load MySQL settings if needed
        if (storageType.equals("mysql")) {
            host = config.getString("storage.mysql.host", "localhost");
            port = config.getInt("storage.mysql.port", 3306);
            database = config.getString("storage.mysql.database", "xshards");
            username = config.getString("storage.mysql.user", "root");
            password = config.getString("storage.mysql.password", "password");
        } else {
            // Load SQLite settings
            sqliteFile = config.getString("storage.sqlite.file", "plugins/XShards/storage/xshards.db");
        }
    }

    // Track if we've already logged the connection message
    private boolean connectionLogged = false;
    
    /**
     * Connect to the database
     * This method connects to either MySQL or SQLite based on the configuration
     */
    public void connect() {
        try {
            if (storageType.equals("mysql")) {
                connectToMySQL();
            } else {
                connectToSQLite();
            }
            connected = true;
            
            // Only log the connection message once
            if (!connectionLogged) {
                plugin.getLogger().info("Successfully connected to " + storageType.toUpperCase() + " database");
                connectionLogged = true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void connectToMySQL() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8",
                    username, password);
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("MySQL JDBC driver not found!");
                throw new SQLException("MySQL JDBC driver not found!");
            }
        }
    }

    private void connectToSQLite() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            try {
                Class.forName("org.sqlite.JDBC");
                
                // Ensure the directory exists
                File dbFile = new File(sqliteFile);
                File parentDir = dbFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
                
                // Enable foreign keys for SQLite
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON;");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("SQLite JDBC driver not found!");
                throw new SQLException("SQLite JDBC driver not found!");
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Get a database connection
     * This method reuses the existing connection if it's valid
     * 
     * @return A database connection
     * @throws SQLException If a database error occurs
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
            return connection;
        }
        
        // Test connection validity and reconnect if needed
        try {
            if (!connection.isValid(1)) {
                plugin.getLogger().info("Database connection lost. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            // If isValid throws an exception, try to reconnect
            connect();
        }
        
        return connection;
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
        try (Statement statement = getConnection().createStatement()) {
            // Player Shards Table
            statement.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "shards INT NOT NULL DEFAULT 0" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // Shop Items Table
            statement.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INT PRIMARY KEY, " +
                    "item_data MEDIUMBLOB NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "display_name VARCHAR(256), " +
                    "lore LONGTEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // AFK Locations Table
            statement.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // Player Locations Table
            statement.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

            // AFK Status Table
            statement.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "is_afk BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "start_time BIGINT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        }
    }

    private void createSQLiteTables() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            // Player Shards Table
            statement.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "shards INTEGER NOT NULL DEFAULT 0" +
                    ");");

            // Shop Items Table
            statement.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INTEGER PRIMARY KEY, " +
                    "item_data BLOB NOT NULL, " +
                    "price REAL NOT NULL, " +
                    "display_name TEXT, " +
                    "lore TEXT" +
                    ");");

            // AFK Locations Table
            statement.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL" +
                    ");");

            // Player Locations Table
            statement.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL" +
                    ");");

            // AFK Status Table
            statement.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "is_afk INTEGER NOT NULL DEFAULT 0, " +
                    "start_time INTEGER" +
                    ");");
        }
    }

    // Utility methods for serializing/deserializing Bukkit objects
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

    // Migration methods removed as we're not using YAML files anymore

    public String getStorageType() {
        return storageType;
    }

    public boolean isConnected() {
        return connected;
    }
}