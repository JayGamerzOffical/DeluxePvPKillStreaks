package org.JayGamerz.database;

import org.JayGamerz.KillStreakPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    
    private final KillStreakPlugin plugin;
    private Connection connection;
    private final String dbPath;
    
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
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // Create table if it doesn't exist
            createTable();
            
            plugin.getLogger().info("Database connection established!");
            
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
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
        String sql = "INSERT OR REPLACE INTO best_killstreaks (uuid, player_name, best_streak, last_updated) " +
                    "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, newBestStreak);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to update best killstreak for " + playerName + ": " + e.getMessage());
        }
    }
    
    public int getBestKillStreak(UUID playerUUID) {
        String sql = "SELECT best_streak FROM best_killstreaks WHERE uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("best_streak");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get best killstreak for UUID " + playerUUID + ": " + e.getMessage());
        }
        
        return 0; // Default if no record found or error
    }
    
    public void loadBestKillStreaks() {
        String sql = "SELECT uuid, best_streak FROM best_killstreaks";
        
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
