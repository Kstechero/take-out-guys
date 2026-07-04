package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.properties.AiProperties;
import com.sky.service.AdminAiChatService;
import com.sky.service.ai.AiToolCallingClient;
import com.sky.service.ai.admin.AdminAiSessionManager;
import com.sky.service.ai.admin.AdminAiToolExecutor;
import com.sky.service.ai.admin.AdminAiToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class AdminAiChatServiceImpl implements AdminAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private static final String TOOL_POLICY = "\nFor current business data or business actions, you must use an available tool. "
            + "Never guess tool results. Only call mutation tools after explicit user confirmation, and only when the tool arguments include confirmed=true.";
    private static final String SYSTEM_PROMPT =
            "You are the admin operations assistant for Takeout Guys AI. "
                    + "Always respond in Chinese. "
                    + "Real-time admin tools are available for shop status, orders, reports, employees, categories, dishes, setmeals, and coupons. "
                    + "Prefer tool results over guesses. "
                    + "Backend tool results are authoritative JSON responses from admin controllers; interpret them directly instead of inventing extra fields. "
                    + "For any mutation such as create, update, delete, toggle, cancel, or password change, ask for explicit confirmation first. "
                    + "When the user has explicitly confirmed, call the mutation tool with confirmed=true. "
                    + "If the user asks about unsupported capabilities such as inventory ledgers, binary file upload from chat, refund finance ledgers, cross-database knowledge retrieval, or automated marketing execution, clearly state the current boundary.";

    private final AiProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final AiToolCallingClient toolCallingClient;
    private final AdminAiToolRegistry toolRegistry;
    private final AdminAiToolExecutor toolExecutor;
    private final AdminAiSessionManager sessionManager;

    public AdminAiChatServiceImpl(AiProperties properties,
                                  com.fasterxml.jackson.databind.ObjectMapper objectMapper,
                                  AiToolCallingClient toolCallingClient,
                                  AdminAiToolRegistry toolRegistry,
                                  AdminAiToolExecutor toolExecutor,
                                  AdminAiSessionManager sessionManager) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.toolCallingClient = toolCallingClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
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
        long startedAt = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "vllm");
        result.put("model", properties.getModel());

        HttpURLConnection connection = null;
        try {
            validateConfiguration();
            connection = (HttpURLConnection) new URL(trimTrailingSlash(properties.getBaseUrl()) + "/models").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(properties.getConnectTimeout());
            connection.setReadTimeout(Math.min(properties.getReadTimeout(), 15000));
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("GX10 returned HTTP " + status);
            }

            com.fasterxml.jackson.databind.JsonNode models = objectMapper.readTree(connection.getInputStream()).path("data");
            boolean found = false;
            if (models.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode model : models) {
                    if (properties.getModel().equals(model.path("id").asText())) {
                        found = true;
                        break;
                    }
                }
            }

            result.put("status", found ? "UP" : "DOWN");
            if (!found) {
                result.put("message", "Target model not found in /models");
            }
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("message", ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        result.put("latencyMs", System.currentTimeMillis() - startedAt);
        result.put("checkedAt", LocalDateTime.now().toString());
        return result;
    }

    private void completeWithTools(AdminAiSessionManager.ChatSession session, String message, Long employeeId,
                                   SseEmitter emitter, AtomicBoolean closed) {
        long startedAt = System.currentTimeMillis();
        try {
            BaseContext.setCurrentId(employeeId);
            String answer = callCompletion(session);
            sessionManager.appendMessage(session, "assistant", answer);
            sessionManager.touchSession(session, answer);
            send(emitter, "meta", mapOf("sessionId", session.getId(), "provider", "vllm", "model", properties.getModel()), closed);
            send(emitter, "delta", Collections.singletonMap("content", answer), closed);
            send(emitter, "done", mapOf("sessionId", session.getId(), "model", properties.getModel(), "durationMs", System.currentTimeMillis() - startedAt), closed);
            emitter.complete();
        } catch (Exception ex) {
            log.error("Admin AI tool calling failed: {}", ex.getMessage(), ex);
            sessionManager.rollbackLastUserMessage(session, message);
            try {
                send(emitter, "error", mapOf("code", "AI_SERVICE_UNAVAILABLE", "message", ex.getMessage()), closed);
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ex);
            }
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    private String callCompletion(AdminAiSessionManager.ChatSession session) throws Exception {
        return toolCallingClient.complete(
                SYSTEM_PROMPT + TOOL_POLICY,
                sessionManager.history(session),
                toolRegistry.adminTools(),
                toolExecutor::execute
        );
    }

    private Long currentEmployeeId() {
        Long employeeId = BaseContext.getCurrentId();
        if (employeeId == null) {
            throw new IllegalStateException("Current employee id is missing");
        }
        return employeeId;
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new IllegalStateException("sky.ai.base-url is not configured");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("GX10_AI_API_KEY is not configured");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("sky.ai.model is not configured");
        }
    }

    private void send(SseEmitter emitter, String event, Object data, AtomicBoolean closed) throws Exception {
        if (!closed.get()) {
            emitter.send(SseEmitter.event().name(event).data(data));
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
