package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.dto.UserAiConfirmationRequestDTO;
import com.sky.service.AdminAiChatService;
import com.sky.service.ai.AgentServiceHttp;
import com.sky.service.ai.admin.AdminAiSessionManager;
import com.sky.vo.UserAiChatResponseVO;
import com.sky.vo.AiSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

@Service
@Slf4j
public class AdminAiChatServiceImpl implements AdminAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private final AgentServiceHttp agentServiceHttp;
    private final AdminAiSessionManager sessionManager;

    public AdminAiChatServiceImpl(AgentServiceHttp agentServiceHttp,
                                  AdminAiSessionManager sessionManager) {
        this.agentServiceHttp = agentServiceHttp;
        this.sessionManager = sessionManager;
    }

    @Override
    public SseEmitter stream(AdminAiChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("Message must not be empty");
        }

        Long employeeId = currentEmployeeId();
        String message = request.getMessage().trim();
        AdminAiSessionManager.ChatSession session = sessionManager.getOrCreateSession(employeeId, request.getSessionId(), message);
        sessionManager.appendMessage(session, "user", message);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean closed = new AtomicBoolean(false);
        Runnable closeEmitter = () -> closed.set(true);
        emitter.onCompletion(closeEmitter);
        emitter.onTimeout(closeEmitter);
        emitter.onError(error -> closeEmitter.run());

        CompletableFuture.runAsync(() -> completeWithTools(session, message, employeeId, emitter, closed));
        return emitter;
    }

    @Override
    public Map<String, Object> health() {
        return agentServiceHttp.health();
    }

    @Override
    public List<AiSessionVO> listSessions() {
        return sessionManager.listSessions(currentEmployeeId());
    }

    @Override
    public List<Map<String, Object>> getSessionMessages(Long sessionId) {
        return sessionManager.history(sessionManager.getOwnedSession(currentEmployeeId(), sessionId));
    }

    @Override
    public void deleteSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        sessionManager.deleteSession(currentEmployeeId(), sessionId);
    }

    @Override
    public UserAiChatResponseVO resume(UserAiConfirmationRequestDTO request) {
        if (request == null || request.getSessionId() == null
                || !StringUtils.hasText(request.getConfirmationToken())
                || !StringUtils.hasText(request.getDecision())) {
            throw new IllegalArgumentException("确认参数不完整");
        }
        String decision = request.getDecision().trim().toLowerCase();
        if (!("approve".equals(decision) || "reject".equals(decision)
                || "edit".equals(decision))) {
            throw new IllegalArgumentException("不支持的确认决定");
        }
        Long employeeId = currentEmployeeId();
        AdminAiSessionManager.ChatSession session = sessionManager.getOwnedSession(
                employeeId, request.getSessionId());
        AgentServiceHttp.Result result = agentServiceHttp.resumeAdmin(
                UUID.randomUUID().toString(), employeeId, session.getId(),
                request.getConfirmationToken(), decision, request.getEditedArguments());
        sessionManager.appendMessage(session, "assistant", result.getAnswer());
        sessionManager.touchSession(session, result.getAnswer());

        UserAiChatResponseVO response = new UserAiChatResponseVO();
        response.setSessionId(session.getId());
        response.setContent(result.getAnswer());
        response.setStatus(result.getStatus());
        response.setConfirmation(result.getConfirmation());
        response.setTraceId(result.getTraceId());
        return response;
    }

    private void completeWithTools(AdminAiSessionManager.ChatSession session, String message, Long employeeId,
                                   SseEmitter emitter, AtomicBoolean closed) {
        long startedAt = System.currentTimeMillis();
        try {
            BaseContext.setCurrentId(employeeId);
            send(emitter, "meta", mapOf("sessionId", session.getId(), "provider", "agent-api"), closed);
            AgentServiceHttp.Result result = agentServiceHttp.adminStream(
                    UUID.randomUUID().toString(),
                    employeeId,
                    session.getId(),
                    message,
                    (event, data) -> {
                        if (!"done".equals(event)) {
                            sendUnchecked(emitter, event, adaptStreamData(event, data), closed);
                        }
                    }
            );
            String answer = result.getAnswer();
            if (!StringUtils.hasText(answer)) {
                sessionManager.rollbackLastUserMessage(session, message);
                emitter.complete();
                return;
            }
            sessionManager.appendMessage(session, "assistant", answer);
            sessionManager.touchSession(session, answer);
            send(emitter, "done", mapOf(
                    "sessionId", session.getId(),
                    "traceId", result.getTraceId(),
                    "durationMs", System.currentTimeMillis() - startedAt
            ), closed);
            emitter.complete();
        } catch (Exception ex) {
            log.error("管理端 AI 工具调用失败：{}", ex.getMessage(), ex);
            sessionManager.rollbackLastUserMessage(session, message);
            try {
                send(emitter, "error", mapOf("code", "AI_SERVICE_UNAVAILABLE", "message", "智能助手暂时不可用"), closed);
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ex);
            }
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    private String callCompletion(AdminAiSessionManager.ChatSession session, String latestMessage) {
        AgentServiceHttp.Result result = agentServiceHttp.adminChat(
                UUID.randomUUID().toString(),
                currentEmployeeId(),
                session.getId(),
                latestMessage
        );
        return result.getAnswer();
    }

    private Long currentEmployeeId() {
        Long employeeId = BaseContext.getCurrentId();
        if (employeeId == null) {
            throw new IllegalStateException("未获取到当前员工");
        }
        return employeeId;
    }

    private void send(SseEmitter emitter, String event, Object data, AtomicBoolean closed) throws Exception {
        if (!closed.get()) {
            emitter.send(SseEmitter.event().name(event).data(data));
        }
    }

    private void sendUnchecked(SseEmitter emitter, String event, Object data, AtomicBoolean closed) {
        try {
            send(emitter, event, data, closed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to forward Agent SSE event", ex);
        }
    }

    private Object adaptStreamData(String event, Object data) {
        if (!(data instanceof Map)) {
            return data;
        }
        Map<String, Object> adapted = new LinkedHashMap<>((Map<String, Object>) data);
        if ("delta".equals(event) && adapted.containsKey("text")) {
            adapted.put("content", adapted.get("text"));
        }
        return adapted;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
