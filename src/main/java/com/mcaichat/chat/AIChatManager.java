package com.mcaichat.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mcaichat.MCAIChatPlugin;
import com.mcaichat.config.Config;
import com.mcaichat.database.DatabaseManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AIChatManager {
    private final MCAIChatPlugin plugin;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Map<UUID, PlayerContextManager> playerContexts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> bannedPlayers = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;
    private String systemPrompt;

    public AIChatManager(MCAIChatPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.databaseManager = plugin.getDatabaseManager();
        loadSystemPrompt();
    }

    private void loadSystemPrompt() {
        Path systemFile = plugin.getDataFolder().toPath().resolve("system.md");
        if (!Files.exists(systemFile)) {
            try (InputStream is = plugin.getResource("system.md")) {
                if (is != null) {
                    Files.copy(is, systemFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save system.md: " + e.getMessage());
            }
        }

        try {
            this.systemPrompt = Files.readString(systemFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to read system.md: " + e.getMessage());
            this.systemPrompt = "You are a helpful AI assistant in Minecraft.";
        }
    }

    private PlayerContextManager getPlayerContext(Player player) {
        UUID playerId = player.getUniqueId();
        return playerContexts.computeIfAbsent(playerId, k -> {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                return databaseManager.loadPlayerContexts(k, plugin.getPluginConfig().getMaxContext());
            }
            return new PlayerContextManager(plugin.getPluginConfig().getMaxContext());
        });
    }

    public void sendMessage(Player player, String message) {
        if (isPlayerBanned(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You have been banned from AI chat!");
            return;
        }

        PlayerContextManager contextManager = getPlayerContext(player);
        ChatHistory history = contextManager.getActiveHistory();
        String contextName = contextManager.getActiveContext();

        history.addUserMessage(message);

        player.sendMessage(ChatColor.WHITE + "| " + message);
        player.sendMessage(ChatColor.GRAY + "AI is thinking.");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String response = callAPI(history);
                history.addAssistantMessage(response);

                if (databaseManager != null && databaseManager.getConnection() != null) {
                    Integer contextId = contextManager.getContextId(contextName);
                    if (contextId != null) {
                        databaseManager.saveMessage(contextId, "user", message);
                        databaseManager.saveMessage(contextId, "assistant", response);
                    }
                }

                sendResponseToPlayer(player, response, contextName);
            } catch (Exception e) {
                plugin.getLogger().severe("AI API error: " + e.getMessage());
                sendResponseToPlayer(player, ChatColor.RED + "| AI error: " + e.getMessage(), contextName);
            }
        });
    }

    private String callAPI(ChatHistory history) throws IOException, InterruptedException {
        Config config = plugin.getPluginConfig();
        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("sk-xxx")) {
            throw new IllegalStateException("API key not configured");
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getModel());

        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        for (ChatHistory.Message msg : history.getMessages()) {
            JsonObject message = new JsonObject();
            message.addProperty("role", msg.getRole());
            message.addProperty("content", msg.getContent());
            messages.add(message);
        }

        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", config.getTemperature());
        requestBody.addProperty("top_p", config.getTopP());
        requestBody.addProperty("presence_penalty", config.getPresencePenalty());
        requestBody.addProperty("frequency_penalty", config.getFrequencyPenalty());

        if (config.getMaxTokens() > 0) {
            requestBody.addProperty("max_tokens", config.getMaxTokens());
        }

        String apiUrl = config.getApi().endsWith("/") ? config.getApi() + "chat/completions"
                : config.getApi() + "/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API request failed with status " + response.statusCode()
                    + ": " + response.body());
        }

        JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
        JsonArray choices = responseBody.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("No choices in response");
        }

        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (message == null) {
            throw new IOException("No message in response");
        }

        return message.get("content").getAsString();
    }

    private void sendResponseToPlayer(Player player, String response, String contextName) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String prefix = ChatColor.WHITE + "| ";

            int maxLength = 256;
            String[] lines = response.split("\n");

            for (String line : lines) {
                while (line.length() > maxLength) {
                    player.sendMessage(prefix + line.substring(0, maxLength));
                    line = line.substring(maxLength);
                }
                player.sendMessage(prefix + line);
            }
        });
    }

    public String createContext(Player player, String name) {
        PlayerContextManager contextManager = getPlayerContext(player);
        if (contextManager.createContext(name)) {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                int contextId = databaseManager.insertContext(player.getUniqueId(), name, true);
                contextManager.setContextId(name, contextId);
                databaseManager.updateActiveContext(player.getUniqueId(), name);
            }
            return ChatColor.GREEN + "Context '" + name + "' created and activated!";
        }
        return ChatColor.RED + "Context '" + name + "' already exists!";
    }

    public String switchContext(Player player, String name) {
        PlayerContextManager contextManager = getPlayerContext(player);
        if (contextManager.switchContext(name)) {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                databaseManager.updateActiveContext(player.getUniqueId(), name);
            }
            return ChatColor.GREEN + "Switched to context '" + name + "'!";
        }
        return ChatColor.RED + "Context '" + name + "' not found!";
    }

    public String deleteContext(Player player, String name) {
        PlayerContextManager contextManager = getPlayerContext(player);
        if (contextManager.deleteContext(name)) {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                databaseManager.deleteContext(player.getUniqueId(), name);
            }
            return ChatColor.GREEN + "Context '" + name + "' deleted! Now in '" + contextManager.getActiveContext() + "'";
        }
        return ChatColor.RED + "Cannot delete context '" + name + "'! (Does it exist or is it 'default'?)";
    }

    public void clearActiveContext(Player player) {
        PlayerContextManager contextManager = getPlayerContext(player);
        String contextName = contextManager.getActiveContext();
        contextManager.clearActiveContext();

        if (databaseManager != null && databaseManager.getConnection() != null) {
            Integer contextId = contextManager.getContextId(contextName);
            if (contextId != null) {
                databaseManager.clearContextMessages(contextId);
            }
        }
    }

    public String listContexts(Player player) {
        PlayerContextManager contextManager = getPlayerContext(player);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GOLD).append("=== Chat Contexts ===").append("\n");
        sb.append(ChatColor.YELLOW).append("Active: ").append(ChatColor.WHITE).append(contextManager.getActiveContext()).append("\n");
        sb.append(ChatColor.GRAY).append("---\n");
        for (String name : contextManager.getContextNames()) {
            if (name.equals(contextManager.getActiveContext())) {
                sb.append(ChatColor.GREEN).append("* ").append(name).append("\n");
            } else {
                sb.append(ChatColor.WHITE).append("  ").append(name).append("\n");
            }
        }
        return sb.toString();
    }

    public void clearHistory(Player player) {
        PlayerContextManager contextManager = getPlayerContext(player);
        contextManager.clearAll();
    }

    public boolean isPlayerBanned(UUID playerId) {
        return bannedPlayers.getOrDefault(playerId, false);
    }

    public void banPlayer(UUID playerId) {
        bannedPlayers.put(playerId, true);
    }

    public void unbanPlayer(UUID playerId) {
        bannedPlayers.remove(playerId);
    }

    public void shutdown() {
        playerContexts.clear();
        bannedPlayers.clear();
    }
}
