package com.sky.service.ai.user;

import com.sky.vo.AiSessionVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class UserAiSessionManager {

    private static final int MAX_SESSIONS_PER_USER = 50;
    private static final int MAX_HISTORY_MESSAGES = 50;
    private static final int MAX_TITLE_LENGTH = 50;

    private final AtomicLong sessionIdGenerator = new AtomicLong(1000);
    private final Map<Long, Map<Long, ChatSession>> userSessions = new ConcurrentHashMap<>();

    public ChatSession getOrCreateSession(Long userId, Long sessionId, String firstMessage) {
        Map<Long, ChatSession> sessions = userSessions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        if (sessionId != null) {
            ChatSession existing = sessions.get(sessionId);
            if (existing != null) {
                return existing;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setId(sessionIdGenerator.incrementAndGet());
        session.setUserId(userId);
        session.setTitle(buildTitle(firstMessage));
        session.setCreateTime(now);
        session.setUpdateTime(now);
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

    public List<AiSessionVO> listSessions(Long userId) {
        Map<Long, ChatSession> sessions = userSessions.getOrDefault(userId, java.util.Collections.emptyMap());
        List<AiSessionVO> result = new ArrayList<>();
        for (ChatSession session : sessions.values()) {
            result.add(toSessionVO(session));
        }
        result.sort(Comparator.comparing(AiSessionVO::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public void deleteSession(Long userId, Long sessionId) {
        Map<Long, ChatSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
    }

    private void trimSessions(Map<Long, ChatSession> sessions) {
        if (sessions.size() <= MAX_SESSIONS_PER_USER) {
            return;
        }
        ChatSession oldest = sessions.values().stream()
                .min(Comparator.comparing(ChatSession::getUpdateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (oldest != null) {
            sessions.remove(oldest.getId());
        }
    }

    private AiSessionVO toSessionVO(ChatSession session) {
        AiSessionVO vo = new AiSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setLastMessage(session.getLastMessage());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private String buildTitle(String message) {
        String normalized = message == null ? "" : message.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return "新会话";
        }
        return normalized.length() <= MAX_TITLE_LENGTH ? normalized : normalized.substring(0, MAX_TITLE_LENGTH) + "...";
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
        private Long userId;
        private String title;
        private String lastMessage;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private final List<ChatMessage> messages = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
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
