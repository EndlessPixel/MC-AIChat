package com.mcaichat;

import co.aikar.commands.PaperCommandManager;
import com.mcaichat.chat.AIChatManager;
import com.mcaichat.command.AICommand;
import com.mcaichat.config.Config;
import com.mcaichat.config.DbConfig;
import com.mcaichat.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MCAIChatPlugin extends JavaPlugin {
    private Config config;
    private DbConfig dbConfig;
    private DatabaseManager databaseManager;
    private AIChatManager chatManager;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        this.config = new Config(this);
        this.dbConfig = new DbConfig(this);
        this.databaseManager = new DatabaseManager(this);
        
        if (databaseManager.connect()) {
            getLogger().info("Database connection established!");
        } else {
            getLogger().severe("Failed to connect to database! Plugin may not work correctly.");
        }

        this.chatManager = new AIChatManager(this);

        registerCommands();
        getLogger().info("MC-AIChat plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (chatManager != null) {
            chatManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("MC-AIChat plugin disabled!");
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new AICommand(this));
    }

    public Config getPluginConfig() {
        return config;
    }

    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AIChatManager getChatManager() {
        return chatManager;
    }
}
