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
import java.util.ArrayList;
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
            player.sendMessage(ChatColor.RED + plugin.getLangManager().get(player.getUniqueId(), "banned"));
            return;
        }

        PlayerContextManager contextManager = getPlayerContext(player);
        ChatHistory history = contextManager.getActiveHistory();
        String contextName = contextManager.getActiveContext();

        history.addUserMessage(message);

        player.sendMessage(ChatColor.WHITE + "| " + message);
        player.sendMessage(ChatColor.GRAY + plugin.getLangManager().get(player.getUniqueId(), "thinking"));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                APIResult result = callAPI(history);
                history.addAssistantMessage(result.response);

                plugin.getLogger().info(String.format("[AI] Player: %s | Context: %s | Prompt Tokens: %d | Completion Tokens: %d | Total Tokens: %d",
                        player.getName(), contextName, result.promptTokens, result.completionTokens, result.totalTokens));

                if (databaseManager != null && databaseManager.getConnection() != null) {
                    Integer contextId = contextManager.getContextId(contextName);
                    if (contextId != null) {
                        databaseManager.saveMessage(contextId, "user", message);
                        databaseManager.saveMessage(contextId, "assistant", result.response);
                    }
                }

                sendResponseToPlayer(player, result.response, contextName);
            } catch (Exception e) {
                plugin.getLogger().severe("AI API error: " + e.getMessage());
                sendResponseToPlayer(player, ChatColor.RED + "| " + plugin.getLangManager().get(player.getUniqueId(), "ai_error").replace("{0}", e.getMessage()), contextName);
            }
        });
    }

    private APIResult callAPI(ChatHistory history) throws IOException, InterruptedException {
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

        int promptTokens = 0;
        int completionTokens = 0;
        int totalTokens = 0;

        JsonObject usage = responseBody.getAsJsonObject("usage");
        if (usage != null) {
            if (usage.has("prompt_tokens")) {
                promptTokens = usage.get("prompt_tokens").getAsInt();
            }
            if (usage.has("completion_tokens")) {
                completionTokens = usage.get("completion_tokens").getAsInt();
            }
            if (usage.has("total_tokens")) {
                totalTokens = usage.get("total_tokens").getAsInt();
            }
        }

        return new APIResult(message.get("content").getAsString(), promptTokens, completionTokens, totalTokens);
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
            return ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "context_created").replace("{0}", name);
        }
        return ChatColor.RED + plugin.getLangManager().get(player.getUniqueId(), "context_exists").replace("{0}", name);
    }

    public String switchContext(Player player, String name) {
        PlayerContextManager contextManager = getPlayerContext(player);
        
        if (contextManager.switchContext(name)) {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                databaseManager.updateActiveContext(player.getUniqueId(), name);
            }
            return ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "context_switched").replace("{0}", name);
        }

        String fuzzyMatch = findFuzzyContext(contextManager, name);
        if (fuzzyMatch != null) {
            contextManager.switchContext(fuzzyMatch);
            if (databaseManager != null && databaseManager.getConnection() != null) {
                databaseManager.updateActiveContext(player.getUniqueId(), fuzzyMatch);
            }
            return ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "context_switched").replace("{0}", fuzzyMatch);
        }

        return ChatColor.RED + plugin.getLangManager().get(player.getUniqueId(), "context_not_found").replace("{0}", name);
    }

    private String findFuzzyContext(PlayerContextManager contextManager, String input) {
        String lowerInput = input.toLowerCase();
        String bestMatch = null;
        int bestScore = Integer.MAX_VALUE;

        for (String contextName : contextManager.getContextNames()) {
            String lowerName = contextName.toLowerCase();
            if (lowerName.contains(lowerInput)) {
                int score = Math.abs(lowerName.length() - lowerInput.length());
                if (score < bestScore) {
                    bestScore = score;
                    bestMatch = contextName;
                }
            }
        }

        return bestMatch;
    }

    public String deleteContext(Player player, String name) {
        PlayerContextManager contextManager = getPlayerContext(player);
        
        String targetName = name;
        if (!contextManager.getContextNames().contains(name)) {
            String fuzzyMatch = findFuzzyContext(contextManager, name);
            if (fuzzyMatch != null) {
                targetName = fuzzyMatch;
            }
        }

        if (contextManager.deleteContext(targetName)) {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                databaseManager.deleteContext(player.getUniqueId(), targetName);
            }
            return ChatColor.GREEN + plugin.getLangManager().get(player.getUniqueId(), "context_deleted")
                    .replace("{0}", targetName)
                    .replace("{1}", contextManager.getActiveContext());
        }
        return ChatColor.RED + plugin.getLangManager().get(player.getUniqueId(), "context_cannot_delete").replace("{0}", targetName);
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
        sb.append(ChatColor.GOLD).append(plugin.getLangManager().get(player.getUniqueId(), "context_list_title")).append("\n");
        sb.append(ChatColor.YELLOW).append(plugin.getLangManager().get(player.getUniqueId(), "context_list_active").replace("{0}", contextManager.getActiveContext())).append("\n");
        sb.append(ChatColor.GRAY).append("---\n");
        for (String name : contextManager.getContextNames()) {
            if (name.equals(contextManager.getActiveContext())) {