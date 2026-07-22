package com.chaeulso.punish.database;

import com.chaeulso.punish.PunishPlugin;
import com.chaeulso.punish.model.Punishment;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Handles professional HikariCP database connectivity and asynchronous queries.
 */
public class DatabaseManager {
    private final PunishPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(@NotNull PunishPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration dbConfig = plugin.getConfigManager().getDatabase();
        String type = dbConfig.getString("type", "SQLite");

        HikariConfig config = new HikariConfig();

        if ("SQLite".equalsIgnoreCase(type)) {
            File dbFile = new File(plugin.getDataFolder(), "punish.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1); // SQLite is single-threaded write
        } else {
            String host = dbConfig.getString("mysql.host", "localhost");
            int port = dbConfig.getInt("mysql.port", 3306);
            String db = dbConfig.getString("mysql.database", "punish");
            String user = dbConfig.getString("mysql.username", "root");
            String pass = dbConfig.getString("mysql.password", "");
            boolean ssl = dbConfig.getBoolean("mysql.ssl", false);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(dbConfig.getInt("mysql.pool-settings.maximum-pool-size", 10));
        }

        config.setPoolName("PunishPool");
        this.dataSource = new HikariDataSource(config);

        // Run migrations/table creation
        createTables();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Punishments Table
            stmt.execute("CREATE TABLE IF NOT EXISTS punishments (" +
                    "id INTEGER PRIMARY KEY " + (dataSource.getJdbcUrl().contains("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                    "punishment_id VARCHAR(36) NOT NULL, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "username VARCHAR(16) NOT NULL, " +
                    "staff_uuid VARCHAR(36) NOT NULL, " +
                    "staff_name VARCHAR(16) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "type VARCHAR(16) NOT NULL, " +
                    "issued BIGINT NOT NULL, " +
                    "expires BIGINT NOT NULL, " +
                    "timezone VARCHAR(32) NOT NULL, " +
                    "server VARCHAR(32) NOT NULL, " +
                    "active BOOLEAN NOT NULL, " +
                    "removed BOOLEAN NOT NULL, " +
                    "removed_by VARCHAR(16), " +
                    "removed_reason TEXT, " +
                    "removed_date BIGINT" +
                    ");");

            // 2. Warns Table
            stmt.execute("CREATE TABLE IF NOT EXISTS warns (" +
                    "id INTEGER PRIMARY KEY " + (dataSource.getJdbcUrl().contains("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                    "warn_id VARCHAR(36) NOT NULL, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "staff_name VARCHAR(16) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "date BIGINT NOT NULL" +
                    ");");

            // 3. Logs Table
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (" +
                    "id INTEGER PRIMARY KEY " + (dataSource.getJdbcUrl().contains("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                    "staff VARCHAR(16) NOT NULL, " +
                    "action VARCHAR(32) NOT NULL, " +
                    "target VARCHAR(16) NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "date BIGINT NOT NULL" +
                    ");");

            // Indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_uuid ON punishments(uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_id ON punishments(punishment_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_warns_uuid ON warns(uuid);");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to run database migrations", e);
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // --- Async DB Queries Operations ---

    public CompletableFuture<Void> savePunishment(@NotNull Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO punishments (punishment_id, uuid, username, staff_uuid, staff_name, reason, type, issued, expires, timezone, server, active, removed, removed_by, removed_reason, removed_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, punishment.getPunishmentId());
                ps.setString(2, punishment.getUuid().toString());
                ps.setString(3, punishment.getUsername());
                ps.setString(4, punishment.getStaffUuid().toString());
                ps.setString(5, punishment.getStaffName());
                ps.setString(6, punishment.getReason());
                ps.setString(7, punishment.getType().name());
                ps.setLong(8, punishment.getIssued());
                ps.setLong(9, punishment.getExpires());
                ps.setString(10, punishment.getTimezone());
                ps.setString(11, punishment.getServer());
                ps.setBoolean(12, punishment.isActive());
                ps.setBoolean(13, punishment.isRemoved());
                ps.setString(14, punishment.getRemovedBy());
                ps.setString(15, punishment.getRemovedReason());
                ps.setLong(16, punishment.getRemovedDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save punishment asynchronous", e);
            }
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<Void> updatePunishment(@NotNull Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE punishments SET active = ?, removed = ?, removed_by = ?, removed_reason = ?, removed_date = ? WHERE punishment_id = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, punishment.isActive());
                ps.setBoolean(2, punishment.isRemoved());
                ps.setString(3, punishment.getRemovedBy());
                ps.setString(4, punishment.getRemovedReason());
                ps.setLong(5, punishment.getRemovedDate());
                ps.setString(6, punishment.getPunishmentId());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update punishment asynchronous", e);
            }
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<List<Punishment>> getActivePunishments(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE uuid = ? AND active = 1;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch active punishments", e);
            }
            return list;
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE uuid = ? ORDER BY issued DESC;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch punishment history", e);
            }
            return list;
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<List<Punishment>> getAllActivePunishments() {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> list = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE active = 1;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch all active punishments", e);
            }
            return list;
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        Punishment p = new Punishment(
                rs.getString("punishment_id"),
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                UUID.fromString(rs.getString("staff_uuid")),
                rs.getString("staff_name"),
                rs.getString("reason"),
                Punishment.Type.valueOf(rs.getString("type")),
                rs.getLong("issued"),
                rs.getLong("expires"),
                rs.getString("timezone"),
                rs.getString("server")
        );
        p.setActive(rs.getBoolean("active"));
        p.setRemoved(rs.getBoolean("removed"));
        p.setRemovedBy(rs.getString("removed_by"));
        p.setRemovedReason(rs.getString("removed_reason"));
        p.setRemovedDate(rs.getLong("removed_date"));
        return p;
    }

    // --- Warns Queries ---

    public CompletableFuture<Void> addWarn(@NotNull String warnId, @NotNull UUID uuid, @NotNull String staff, @NotNull String reason) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO warns (warn_id, uuid, staff_name, reason, date) VALUES (?, ?, ?, ?, ?);";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, warnId);
                ps.setString(2, uuid.toString());
                ps.setString(3, staff);
                ps.setString(4, reason);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save warning", e);
            }
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<Integer> getWarnCount(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) AS total FROM warns WHERE uuid = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to fetch warning count", e);
            }
            return 0;
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<Void> clearWarns(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM warns WHERE uuid = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear warnings", e);
            }
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    public CompletableFuture<Boolean> deleteWarn(@NotNull UUID uuid, @NotNull String warnId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM warns WHERE uuid = ? AND warn_id = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, warnId);
                int deleted = ps.executeUpdate();
                return deleted > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete warning", e);
            }
            return false;
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }

    // --- Action Logging Queries ---

    public CompletableFuture<Void> saveLog(@NotNull String staff, @NotNull String action, @NotNull String target, @NotNull String reason) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO logs (staff, action, target, reason, date) VALUES (?, ?, ?, ?, ?);";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, staff);
                ps.setString(2, action);
                ps.setString(3, target);
                ps.setString(4, reason);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mod log", e);
            }
        }, task -> plugin.getAuthScheduler().runTaskAsynchronously(task));
    }
}
