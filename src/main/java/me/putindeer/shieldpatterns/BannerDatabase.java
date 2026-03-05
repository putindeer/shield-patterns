package me.putindeer.shieldpatterns;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BannerDatabase {
    private final Main plugin;
    private final Connection connection;

    public BannerDatabase(Main plugin) {
        this.plugin = plugin;
        try {
            File file = new File(plugin.getDataFolder(), "banners.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the "banners" table in the database if it does not already exist.
     * The table includes the following columns:
     * - "uuid" as the primary key in TEXT format, representing a unique identifier.
     * - "patterns" in TEXT format, which stores non-null comma-separated values.
     * <br><br>
     * This method ensures the required schema is present for storing banner data.
     *
     * @throws SQLException if an error occurs while executing the database query
     */
    private void createTable() throws SQLException {
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS banners (
                uuid TEXT PRIMARY KEY,
                patterns TEXT NOT NULL
            );
        """);
    }

    /**
     * Saves the player's patterns to the database immediately.
     * If patterns list is empty, removes the entry instead.
     */
    public void save(UUID uuid, List<String> patterns) {
        if (patterns.isEmpty()) {
            delete(uuid);
            return;
        }

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO banners(uuid, patterns)
            VALUES(?, ?)
            ON CONFLICT(uuid) DO UPDATE SET patterns=excluded.patterns;
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, String.join(",", patterns));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.utils.severe("Failed to save banner for " + uuid + ":");
            plugin.utils.severe(e.getStackTrace());
        }
    }

    /**
     * Loads the player's patterns from the database.
     * Returns an empty list if the player has no entry.
     */
    public List<String> load(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT patterns FROM banners WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return new ArrayList<>();
            String raw = rs.getString("patterns");

            if (raw == null || raw.isEmpty()) return new ArrayList<>();
            return new ArrayList<>(List.of(raw.split(",")));

        } catch (SQLException e) {
            plugin.utils.severe("Failed to load banner for " + uuid + ":");
            plugin.utils.severe(e.getStackTrace());
            return new ArrayList<>();
        }
    }

    /**
     * Deletes the player's entry from the database (used on reset).
     */
    public void delete(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM banners WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.utils.severe("Failed to delete banner for " + uuid + ":");
            plugin.utils.severe(e.getStackTrace());
        }
    }
}