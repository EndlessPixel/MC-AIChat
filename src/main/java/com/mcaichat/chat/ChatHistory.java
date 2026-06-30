package com.mcaichat.chat;

import java.util.ArrayList;
import java.util.List;

public class ChatHistory {
    private final List<Message> messages;
    private final int maxContext;

    public ChatHistory(int maxContext) {
        this.messages = new ArrayList<>();
        this.maxContext = maxContext;
    }

    public void addMessage(String role, String content) {
        messages.add(new Message(role, content));
        trim();
    }

    public void addUserMessage(String content) {
        addMessage("user", content);
    }

    public void addAssistantMessage(String content) {
        addMessage("assistant", content);
    }

    public void clear() {
        messages.clear();
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    private void trim() {
        if (maxContext <= 0) return;
        int maxMessages = maxContext * 2;
        while (messages.size() > maxMessages) {
            messages.remove(0);
        }
    }

    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
