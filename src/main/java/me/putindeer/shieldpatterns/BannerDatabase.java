package me.putindeer.shieldbanners;

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

    private void createTable() throws SQLException {
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS banners (
                uuid TEXT PRIMARY KEY,
                patterns TEXT NOT NULL
            );
        """);
    }

    public void save(UUID uuid, List<String> patterns) {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO banners(uuid, patterns)
            VALUES(?, ?)
            ON CONFLICT(uuid) DO UPDATE SET patterns=excluded.patterns;
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, String.join(",", patterns));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.utils.severe("Failed to save banner:");
            plugin.utils.severe(e.getStackTrace());
        }
    }

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
            plugin.utils.severe("Failed to load banner:");
            plugin.utils.severe(e.getStackTrace());
            return new ArrayList<>();
        }
    }
}