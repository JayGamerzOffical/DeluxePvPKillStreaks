package org.JayGamerz.database;

import org.JayGamerz.KillStreakPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    
    private final KillStreakPlugin plugin;
    private Connection connection;
    private final String dbPath;
    private static final int CONNECTION_TIMEOUT = 5; // seconds
    
    public DatabaseManager(KillStreakPlugin plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + "killstreaks.db";
    }
    
    public void initialize() {
        try {
            // Create data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Create connection
            establishConnection();
            
            // Create table if it doesn't exist
            createTable();
            
            plugin.getLogger().info("Database connection established!");
            
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Establishes a new database connection
     */
    private void establishConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        
        // Enable WAL mode for better performance and concurrency
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA temp_store=MEMORY;");
        }
    }
    
    /**
     * Validates if the current connection is active and valid
     */
    private boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(CONNECTION_TIMEOUT);
        } catch (SQLException e) {
            plugin.getLogger().warning("Connection validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ensures we have a valid connection, reconnecting if necessary
     */
    private void ensureConnection() throws SQLException {
        if (!isConnectionValid()) {
            plugin.getLogger().info("Reconnecting to database...");
            try {
                establishConnection();
            } catch (ClassNotFoundException e) {
                throw new SQLException("Failed to reconnect to database", e);
            }
        }
    }
    
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS best_killstreaks (" +
                    "uuid TEXT PRIMARY KEY," +
                    "player_name TEXT," +
                    "best_streak INTEGER," +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    public void updateBestKillStreak(UUID playerUUID, String playerName, int newBestStreak) {
        // Input validation
        if (playerUUID == null || playerName == null || newBestStreak < 0) {
            plugin.getLogger().warning("Invalid parameters for updateBestKillStreak");
            return;
        }
        
        String sql = "INSERT OR REPLACE INTO best_killstreaks (uuid, player_name, best_streak, last_updated) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        
        try {
            ensureConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, newBestStreak);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to update best killstreak for " + playerName + ": " + e.getMessage());
        }
    }

    public int getBestKillStreak(UUID playerUUID) {
        if (playerUUID == null) {
            plugin.getLogger().warning("PlayerUUID cannot be null in getBestKillStreak");
            return 0;
        }
        
        String sql = "SELECT best_streak FROM best_killstreaks WHERE uuid = ?";
        
        try {
            ensureConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("best_streak");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get best killstreak for UUID " + playerUUID + ": " + e.getMessage());
        }
        
        return 0; // Default if no record found or error
    }
    
    public void loadBestKillStreaks() {
        String sql = "SELECT uuid, best_streak FROM best_killstreaks";
        
        try {
            ensureConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                int loaded = 0;
                while (rs.next()) {
                    String uuidString = rs.getString("uuid");
                    int bestStreak = rs.getInt("best_streak");
                    
                    try {
                        UUID playerUUID = UUID.fromString(uuidString);
                        plugin.setBestKillStreak(playerUUID, bestStreak);
                        loaded++;
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in database: " + uuidString);
                    }
                }
                
                plugin.getLogger().info("Loaded " + loaded + " best killstreak records from database!");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load best killstreaks from database: " + e.getMessage());
        }
    }
    
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed!");
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to close database connection: " + e.getMessage());
            }
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
