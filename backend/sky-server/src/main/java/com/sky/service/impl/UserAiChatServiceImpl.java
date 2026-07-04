package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.UserAiChatRequestDTO;
import com.sky.properties.AiProperties;
import com.sky.service.UserAiChatService;
import com.sky.service.ai.AiToolCallingClient;
import com.sky.service.ai.user.UserAiSessionManager;
import com.sky.service.ai.user.UserAiToolExecutor;
import com.sky.service.ai.user.UserAiToolRegistry;
import com.sky.vo.AiSessionVO;
import com.sky.vo.UserAiChatResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class UserAiChatServiceImpl implements UserAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private static final String TOOL_POLICY = "\nFor current business data or business actions, you must use an available tool. "
            + "Never guess tool results. Only call mutation tools after explicit user confirmation, and only when the tool arguments include confirmed=true.";
    private static final String SYSTEM_PROMPT =
            "你是 Takeout Guys 的用户端智能客服“小暖”。请始终使用简洁、友好的中文回复。"
                    + "对于店铺状态、菜单、订单、购物车、优惠券和地址等当前业务数据或动作，必须优先使用工具，不要猜测。"
                    + "工具会返回真实结构化 JSON 数据，请基于这些结果回答。"
                    + "任何会修改用户数据的动作，例如领券、修改地址、清空购物车、取消订单、再来一单或催单，都必须先征得用户明确确认，再调用对应工具并传 confirmed=true。"
                    + "如果用户询问当前未接入的能力，例如真实人工会话转接、复杂售后退款进度、知识库检索等，请明确说明边界。";

    private final AiProperties properties;
    private final AiToolCallingClient toolCallingClient;
    private final UserAiToolRegistry toolRegistry;
    private final UserAiToolExecutor toolExecutor;
    private final UserAiSessionManager sessionManager;

    public UserAiChatServiceImpl(AiProperties properties,
                                 AiToolCallingClient toolCallingClient,
                                 UserAiToolRegistry toolRegistry,
                                 UserAiToolExecutor toolExecutor,
                                 UserAiSessionManager sessionManager) {
        this.properties = properties;
        this.toolCallingClient = toolCallingClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.sessionManager = sessionManager;
    }

    @Override
    public UserAiChatResponseVO chat(UserAiChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("消息不能为空");
        }

        Long userId = currentUserId();
        String message = request.getMessage().trim();
        UserAiSessionManager.ChatSession session = sessionManager.getOrCreateSession(userId, request.getSessionId(), message);
        sessionManager.appendMessage(session, "user", message);
        try {
            BaseContext.setCurrentId(userId);
            String content = callCompletion(session);
            sessionManager.appendMessage(session, "assistant", content);
            sessionManager.touchSession(session, content);
            UserAiChatResponseVO response = new UserAiChatResponseVO();
            response.setSessionId(session.getId());
            response.setContent(content);
            return response;
        } catch (Exception ex) {
            sessionManager.rollbackLastUserMessage(session, message);
            log.error("用户端 AI 对话失败: {}", ex.getMessage(), ex);
            throw new IllegalStateException("智能客服暂时不可用，请稍后再试");
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    @Override
    public SseEmitter stream(Long sessionId, String message) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("消息不能为空");
        }

        Long userId = currentUserId();
        String trimmedMessage = message.trim();
        UserAiSessionManager.ChatSession session = sessionManager.getOrCreateSession(userId, sessionId, trimmedMessage);
        sessionManager.appendMessage(session, "user", trimmedMessage);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicBoolean closed = new AtomicBoolean(false);
        Runnable closeEmitter = () -> closed.set(true);
        emitter.onCompletion(closeEmitter);
        emitter.onTimeout(closeEmitter);
        emitter.onError(error -> closeEmitter.run());

        CompletableFuture.runAsync(() -> completeWithTools(session, trimmedMessage, userId, emitter, closed));
        return emitter;
    }

    @Override
    public List<AiSessionVO> listSessions() {
        return sessionManager.listSessions(currentUserId());
    }

    @Override
    public void deleteSession(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        sessionManager.deleteSession(currentUserId(), sessionId);
    }

    private void completeWithTools(UserAiSessionManager.ChatSession session, String message, Long userId,
                                   SseEmitter emitter, AtomicBoolean closed) {
        long startedAt = System.currentTimeMillis();
        try {
            BaseContext.setCurrentId(userId);
            String answer = callCompletion(session);
            sessionManager.appendMessage(session, "assistant", answer);
            sessionManager.touchSession(session, answer);
            send(emitter, "meta", mapOf("sessionId", session.getId(), "provider", "vllm", "model", properties.getModel()), closed);
            send(emitter, "delta", Collections.singletonMap("content", answer), closed);
            send(emitter, "done", mapOf("sessionId", session.getId(), "durationMs", System.currentTimeMillis() - startedAt), closed);
            emitter.complete();
        } catch (Exception ex) {
            log.error("User AI tool calling failed: {}", ex.getMessage(), ex);
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

    private String callCompletion(UserAiSessionManager.ChatSession session) throws Exception {
        return toolCallingClient.complete(
                SYSTEM_PROMPT + TOOL_POLICY,
                sessionManager.history(session),
                toolRegistry.userTools(),
                toolExecutor::execute
        );
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new IllegalStateException("未获取到当前用户");
        }
        return userId;
    }

    private void send(SseEmitter emitter, String event, Object data, AtomicBoolean closed) throws Exception {
        if (!closed.get()) {
            emitter.send(SseEmitter.event().name(event).data(data));
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
