package com.mcaichat.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlayerContextManager {
    private final Map<String, ChatHistory> contexts = new HashMap<>();
    private final Map<String, Integer> contextIds = new HashMap<>();
    private String activeContext;
    private final int maxContext;

    public PlayerContextManager(int maxContext) {
        this.maxContext = maxContext;
        this.activeContext = "default";
        contexts.put("default", new ChatHistory(maxContext));
    }

    public ChatHistory getActiveHistory() {
        return contexts.get(activeContext);
    }

    public String getActiveContext() {
        return activeContext;
    }

    public void setActiveContext(String activeContext) {
        this.activeContext = activeContext;
    }

    public boolean switchContext(String name) {
        if (contexts.containsKey(name)) {
            activeContext = name;
            return true;
        }
        return false;
    }

    public boolean createContext(String name) {
        if (contexts.containsKey(name)) {
            return false;
        }
        contexts.put(name, new ChatHistory(maxContext));
        activeContext = name;
        return true;
    }

    public boolean deleteContext(String name) {
        if ("default".equals(name)) {
            return false;
        }
        if (!contexts.containsKey(name)) {
            return false;
        }
        contexts.remove(name);
        contextIds.remove(name);
        if (activeContext.equals(name)) {
            activeContext = "default";
        }
        return true;
    }

    public void clearActiveContext() {
        ChatHistory history = contexts.get(activeContext);
        if (history != null) {
            history.clear();
        }
    }

    public void clearAll() {
        contexts.clear();
        contextIds.clear();
        activeContext = "default";
        contexts.put("default", new ChatHistory(maxContext));
    }

    public Set<String> getContextNames() {
        return contexts.keySet();
    }

    public int getContextCount() {
        return contexts.size();
    }

    public Map<String, ChatHistory> getContexts() {
        return contexts;
    }

    public Map<String, Integer> getContextIds() {
        return contextIds;
    }

    public Integer getContextId(String name) {
        return contextIds.get(name);
    }

    public void setContextId(String name, int id) {
        contextIds.put(name, id);
    }
}
