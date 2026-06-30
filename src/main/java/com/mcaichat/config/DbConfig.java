package com.mcaichat.config;

import com.mcaichat.MCAIChatPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class DbConfig {
    private final MCAIChatPlugin plugin;
    private String dbFile;
    private int autoSaveInterval;
    private boolean saveOnQuit;
    private int maxHistoryPerContext;

    public DbConfig(MCAIChatPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "db.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        try (InputStream is = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            this.dbFile = getString(data, "db_file", "data/mcaichat.db");
            this.autoSaveInterval = getInt(data, "auto_save_interval", 30);
            this.saveOnQuit = getBoolean(data, "save_on_quit", true);
            this.maxHistoryPerContext = getInt(data, "max_history_per_context", 100);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load db.yml: " + e.getMessage());
        }
    }

    private void saveDefaultConfig() {
        plugin.getDataFolder().mkdirs();
        File configFile = new File(plugin.getDataFolder(), "db.yml");
        try (InputStream is = plugin.getResource("db.yml")) {
            if (is != null) {
                Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save default db.yml: " + e.getMessage());
        }
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String getDbFile() {
        return dbFile;
    }

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public boolean isSaveOnQuit() {
        return saveOnQuit;
    }

    public int getMaxHistoryPerContext() {
        return maxHistoryPerContext;
    }
}
