package com.mcaichat.lang;

import com.mcaichat.MCAIChatPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LangManager {
    private final MCAIChatPlugin plugin;
    private final Map<String, Map<String, String>> langFiles = new HashMap<>();
    private final Map<UUID, String> playerLangs = new ConcurrentHashMap<>();
    private String defaultLang;

    public LangManager(MCAIChatPlugin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    private void loadLanguages() {
        File langsDir = new File(plugin.getDataFolder(), "langs");
        if (!langsDir.exists()) {
            langsDir.mkdirs();
            saveDefaultLangs();
        }

        defaultLang = plugin.getPluginConfig().getDefaultLang();

        for (File file : langsDir.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                String langCode = file.getName().replace(".yml", "");
                loadLangFile(langCode, file);
            }
        }
    }

    private void saveDefaultLangs() {
        saveLangResource("zh-CN.yml");
        saveLangResource("en.yml");
        saveLangResource("jp.yml");
    }

    private void saveLangResource(String fileName) {
        try (InputStream is = plugin.getResource("langs/" + fileName)) {
            if (is != null) {
                File target = new File(plugin.getDataFolder(), "langs/" + fileName);
                Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save lang file: " + fileName);
        }
    }

    private void loadLangFile(String langCode, File file) {
        try (InputStream is = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, String> langData = yaml.load(is);
            langFiles.put(langCode, langData);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load lang file: " + langCode);
        }
    }

    public String get(UUID playerId, String key) {
        String langCode = playerLangs.getOrDefault(playerId, defaultLang);
        Map<String, String> langData = langFiles.get(langCode);
        if (langData == null) {
            langData = langFiles.get("en");
        }
        if (langData == null) {
            return key;
        }
        return langData.getOrDefault(key, key);
    }

    public void setPlayerLang(UUID playerId, String langCode) {
        if (langFiles.containsKey(langCode)) {
            playerLangs.put(playerId, langCode);
        }
    }

    public String getPlayerLang(UUID playerId) {
        return playerLangs.getOrDefault(playerId, defaultLang);
    }

    public boolean isValidLang(String langCode) {
        return langFiles.containsKey(langCode);
    }

    public String[] getAvailableLangs() {
        return langFiles.keySet().toArray(new String[0]);
    }

    public void reload() {
        langFiles.clear();
        playerLangs.clear();
        loadLanguages();
    }
}
