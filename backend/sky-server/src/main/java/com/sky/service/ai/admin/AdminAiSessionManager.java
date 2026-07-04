package com.sky.service.ai.admin;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AdminAiSessionManager {

    private static final int MAX_SESSIONS_PER_EMPLOYEE = 50;
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final AtomicLong sessionIdGenerator = new AtomicLong(1000);
    private final Map<Long, Map<Long, ChatSession>> employeeSessions = new ConcurrentHashMap<>();

    public ChatSession getOrCreateSession(Long employeeId, Long sessionId, String firstMessage) {
        Map<Long, ChatSession> sessions = employeeSessions.computeIfAbsent(employeeId, key -> new ConcurrentHashMap<>());
        if (sessionId != null) {
            ChatSession existing = sessions.get(sessionId);
            if (existing != null) {
                return existing;
            }
        }

        ChatSession session = new ChatSession();
        session.setId(sessionIdGenerator.incrementAndGet());
        session.setEmployeeId(employeeId);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(session.getCreateTime());
        session.setLastMessage(firstMessage);
        sessions.put(session.getId(), session);
        trimSessions(sessions);
        return session;
    }

    public void appendMessage(ChatSession session, String role, String content) {
        synchronized (session) {
            session.getMessages().add(new ChatMessage(role, content));
            while (session.getMessages().size() > MAX_HISTORY_MESSAGES) {
                session.getMessages().remove(0);
            }
        }
    }

    public void rollbackLastUserMessage(ChatSession session, String content) {
        synchronized (session) {
            List<ChatMessage> messages = session.getMessages();
            if (messages.isEmpty()) {
                return;
            }
            ChatMessage last = messages.get(messages.size() - 1);
            if (Objects.equals(last.getRole(), "user") && Objects.equals(last.getContent(), content)) {
                messages.remove(messages.size() - 1);
            }
        }
    }

    public void touchSession(ChatSession session, String lastMessage) {
        session.setLastMessage(lastMessage);
        session.setUpdateTime(LocalDateTime.now());
    }

    public List<Map<String, Object>> history(ChatSession session) {
        List<Map<String, Object>> history = new ArrayList<>();
        synchronized (session) {
            for (ChatMessage item : session.getMessages()) {
                history.add(mapOf("role", item.getRole(), "content", item.getContent()));
            }
        }
        return history;
    }

    private void trimSessions(Map<Long, ChatSession> sessions) {
        if (sessions.size() <= MAX_SESSIONS_PER_EMPLOYEE) {
            return;
        }
        ChatSession oldest = sessions.values().stream()
                .min((left, right) -> left.getUpdateTime().compareTo(right.getUpdateTime()))
                .orElse(null);
        if (oldest != null) {
            sessions.remove(oldest.getId());
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    public static class ChatSession {
        private Long id;
        private Long employeeId;
        private String lastMessage;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private final List<ChatMessage> messages = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
        public List<ChatMessage> getMessages() { return messages; }
    }

    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
