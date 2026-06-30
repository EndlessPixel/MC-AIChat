package com.mcaichat.database;

import com.mcaichat.MCAIChatPlugin;
import com.mcaichat.chat.ChatHistory;
import com.mcaichat.chat.PlayerContextManager;
import com.mcaichat.config.DbConfig;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final MCAIChatPlugin plugin;
    private Connection connection;
    private DbConfig dbConfig;

    public DatabaseManager(MCAIChatPlugin plugin) {
        this.plugin = plugin;
        this.dbConfig = plugin.getDbConfig();
    }

    public boolean connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), dbConfig.getDbFile());
            dbFile.getParentFile().mkdirs();
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        String createContextsTable = """
                CREATE TABLE IF NOT EXISTS contexts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    UNIQUE(player_uuid, name)
                )""";

        String createMessagesTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    context_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (context_id) REFERENCES contexts(id) ON DELETE CASCADE
                )""";

        String createContextIndex = "CREATE INDEX IF NOT EXISTS idx_contexts_player_uuid ON contexts(player_uuid)";
        String createMessagesIndex = "CREATE INDEX IF NOT EXISTS idx_messages_context_id ON messages(context_id)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createContextsTable);
            stmt.execute(createMessagesTable);
            stmt.execute(createContextIndex);
            stmt.execute(createMessagesIndex);
        }
    }

    public PlayerContextManager loadPlayerContexts(UUID playerId, int maxContext) {
        PlayerContextManager manager = new PlayerContextManager(maxContext);

        try {
            String selectContexts = "SELECT id, name, is_active FROM contexts WHERE player_uuid = ? ORDER BY created_at";
            try (PreparedStatement stmt = connection.prepareStatement(selectContexts)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    String activeContext = "default";
                    List<Integer> contextIds = new ArrayList<>();

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        boolean isActive = rs.getInt("is_active") == 1;

                        if (isActive) {
                            activeContext = name;
                        }

                        ChatHistory history = new ChatHistory(maxContext);
                        loadContextMessages(id, history);
                        manager.getContexts().put(name, history);
                        manager.getContextIds().put(name, id);
                        contextIds.add(id);
                    }

                    if (manager.getContexts().isEmpty()) {
                        manager.getContextIds().put("default", insertContext(playerId, "default", true));
                    } else {
                        manager.setActiveContext(activeContext);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player contexts: " + e.getMessage());
        }

        return manager;
    }

    private void loadContextMessages(int contextId, ChatHistory history) throws SQLException {
        String selectMessages = "SELECT role, content FROM messages WHERE context_id = ? ORDER BY created_at";
        try (PreparedStatement stmt = connection.prepareStatement(selectMessages)) {
            stmt.setInt(1, contextId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String role = rs.getString("role");
                    String content = rs.getString("content");
                    history.addMessage(role, content);
                }
            }
        }
    }

    public int insertContext(UUID playerId, String name, boolean isActive) {
        try {
            String insert = "INSERT INTO contexts (player_uuid, name, is_active, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, name);
                stmt.setInt(3, isActive ? 1 : 0);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert context: " + e.getMessage());
        }
        return -1;
    }

    public void updateActiveContext(UUID playerId, String contextName) {
        try {
            String deactivateAll = "UPDATE contexts SET is_active = 0 WHERE player_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deactivateAll)) {
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
            }

            String activate = "UPDATE contexts SET is_active = 1 WHERE player_uuid = ? AND name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(activate)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, contextName);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update active context: " + e.getMessage());
        }
    }

    public void deleteContext(UUID playerId, String contextName) {
        try {
            String delete = "DELETE FROM contexts WHERE player_uuid = ? AND name = ?";
            try (PreparedStatement stmt = connection.prepareStatement(delete)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, contextName);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete context: " + e.getMessage());
        }
    }

    public void saveMessage(int contextId, String role, String content) {
        try {
            String insert = "INSERT INTO messages (context_id, role, content, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(insert)) {
                stmt.setInt(1, contextId);
                stmt.setString(2, role);
                stmt.setString(3, content);
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            }

            trimOldMessages(contextId);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save message: " + e.getMessage());
        }
    }

    private void trimOldMessages(int contextId) throws SQLException {
        int maxHistory = dbConfig.getMaxHistoryPerContext();
        if (maxHistory <= 0) return;

        String count = "SELECT COUNT(*) FROM messages WHERE context_id = ?";
        int total;
        try (PreparedStatement stmt = connection.prepareStatement(count)) {
            stmt.setInt(1, contextId);
            try (ResultSet rs = stmt.executeQuery()) {
                total = rs.next() ? rs.getInt(1) : 0;
            }
        }

        if (total > maxHistory) {
            int toDelete = total - maxHistory;
            String delete = "DELETE FROM messages WHERE context_id = ? ORDER BY created_at LIMIT ?";
            try (PreparedStatement stmt = connection.prepareStatement(delete)) {
                stmt.setInt(1, contextId);
                stmt.setInt(2, toDelete);
                stmt.executeUpdate();
            }
        }
    }

    public void clearContextMessages(int contextId) {
        try {
            String delete = "DELETE FROM messages WHERE context_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(delete)) {
                stmt.setInt(1, contextId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to clear context messages: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
