package com.sky.service.ai.admin;

import com.sky.entity.AiChatMessage;
import com.sky.entity.AiChatSession;
import com.sky.mapper.AiChatMessageMapper;
import com.sky.mapper.AiChatSessionMapper;
import com.sky.vo.AiSessionVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AdminAiSessionManager {

    private static final String SCOPE = "admin";
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_LAST_MESSAGE_LENGTH = 480;

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    public AdminAiSessionManager(AiChatSessionMapper sessionMapper, AiChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    public ChatSession getOrCreateSession(Long employeeId, Long sessionId, String firstMessage) {
        if (sessionId != null) {
            AiChatSession existing = sessionMapper.getByIdAndOwner(sessionId, SCOPE, employeeId);
            if (existing != null) {
                return hydrate(existing);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        AiChatSession session = new AiChatSession();
        session.setScope(SCOPE);
        session.setOwnerId(employeeId);
        session.setTitle(normalizeForStorage(firstMessage, MAX_TITLE_LENGTH));
        session.setLastMessage(normalizeForStorage(firstMessage, MAX_LAST_MESSAGE_LENGTH));
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        return hydrate(session);
    }

    public ChatSession getOwnedSession(Long employeeId, Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        AiChatSession existing = sessionMapper.getByIdAndOwner(sessionId, SCOPE, employeeId);
        if (existing == null) {
            throw new IllegalArgumentException("会话不存在或无权访问");
        }
        return hydrate(existing);
    }

    public void appendMessage(ChatSession session, String role, String content) {
        synchronized (session) {
            AiChatMessage message = new AiChatMessage();
            message.setSessionId(session.getId());
            message.setRole(role);
            message.setContent(content);
            message.setCreateTime(LocalDateTime.now());
            messageMapper.insert(message);
            session.getMessages().add(new ChatMessage(message.getId(), role, content));
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
                if (last.getId() != null) {
                    messageMapper.deleteById(last.getId());
                }
                messages.remove(messages.size() - 1);
            }
        }
    }

    public void touchSession(ChatSession session, String lastMessage) {
        String normalized = normalizeForStorage(lastMessage, MAX_LAST_MESSAGE_LENGTH);
        session.setLastMessage(normalized);
        session.setUpdateTime(LocalDateTime.now());
        AiChatSession meta = new AiChatSession();
        meta.setId(session.getId());
        meta.setTitle(normalizeForStorage(session.getTitle(), MAX_TITLE_LENGTH));
        meta.setLastMessage(normalized);
        meta.setUpdateTime(session.getUpdateTime());
        sessionMapper.updateMeta(meta);
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

    public List<AiSessionVO> listSessions(Long employeeId) {
        List<AiSessionVO> result = new ArrayList<>();
        for (AiChatSession session : sessionMapper.listByOwner(SCOPE, employeeId)) {
            AiSessionVO vo = new AiSessionVO();
            vo.setId(session.getId());
            vo.setTitle(session.getTitle());
            vo.setLastMessage(session.getLastMessage());
            vo.setCreateTime(session.getCreateTime());
            vo.setUpdateTime(session.getUpdateTime());
            result.add(vo);
        }
        result.sort(Comparator.comparing(
                AiSessionVO::getUpdateTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result;
    }

    public void deleteSession(Long employeeId, Long sessionId) {
        AiChatSession existing = sessionMapper.getByIdAndOwner(sessionId, SCOPE, employeeId);
        if (existing != null) {
            messageMapper.deleteBySessionId(sessionId);
            sessionMapper.deleteByIdAndOwner(sessionId, SCOPE, employeeId);
        }
    }

    private ChatSession hydrate(AiChatSession entity) {
        ChatSession session = new ChatSession();
        session.setId(entity.getId());
        session.setEmployeeId(entity.getOwnerId());
        session.setTitle(entity.getTitle());
        session.setLastMessage(entity.getLastMessage());
        session.setCreateTime(entity.getCreateTime());
        session.setUpdateTime(entity.getUpdateTime());
        for (AiChatMessage message : messageMapper.listRecentBySessionId(entity.getId(), MAX_HISTORY_MESSAGES)) {
            session.getMessages().add(new ChatMessage(message.getId(), message.getRole(), message.getContent()));
        }
        return session;
    }

    private String normalizeForStorage(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    public static class ChatSession {
        private Long id;
        private Long employeeId;
        private String title;
        private String lastMessage;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private final List<ChatMessage> messages = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
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
        private final Long id;
        private final String role;
        private final String content;

        public ChatMessage(Long id, String role, String content) {
            this.id = id;
            this.role = role;
            this.content = content;
        }

        public Long getId() { return id; }
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
