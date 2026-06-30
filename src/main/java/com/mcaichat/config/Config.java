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

public class Config {
    private final MCAIChatPlugin plugin;
    private String api;
    private String apiKey;
    private boolean envApiKey;
    private String model;
    private double temperature;
    private double topP;
    private double presencePenalty;
    private double frequencyPenalty;
    private int maxTokens;
    private int maxContext;
    private String defaultLang;

    public Config(MCAIChatPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        try (InputStream is = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            this.api = getString(data, "api", "https://api.openai.com/v1");
            this.apiKey = getString(data, "api_key", "sk-xxx");
            this.envApiKey = getBoolean(data, "env_api_key", false);
            this.model = getString(data, "model", "gpt-4o");
            if (data.get("model") == null && data.get("modle") != null) {
                this.model = getString(data, "modle", "gpt-4o");
            }
            this.temperature = getDouble(data, "temperature", 1.0);
            this.topP = getDouble(data, "top_p", 1.0);
            this.presencePenalty = getDouble(data, "presence_penalty", 1.0);
            this.frequencyPenalty = getDouble(data, "frequency_penalty", 1.0);
            this.maxTokens = getInt(data, "max_tokens", 0);
            this.maxContext = getInt(data, "max_context", 20);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }

    private void saveDefaultConfig() {
        plugin.getDataFolder().mkdirs();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try (InputStream is = plugin.getResource("config.yml")) {
            if (is != null) {
                Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save default config: " + e.getMessage());
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

    private double getDouble(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
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

    public String getApi() {
        return api;
    }

    public String getApiKey() {
        if (envApiKey) {
            return System.getenv("MC-AICHAT-KEY");
        }
        return apiKey;
    }

    public boolean isEnvApiKey() {
        return envApiKey;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTopP() {
        return topP;
    }

    public double getPresencePenalty() {
        return presencePenalty;
    }

    public double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getMaxContext() {
        return maxContext;
    }

    public String getDefaultLang() {
        return defaultLang;
    }
}
