package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.AiRecommendRequestDTO;
import com.sky.dto.AiReviewWriteRequestDTO;
import com.sky.dto.UserAiChatRequestDTO;
import com.sky.dto.UserAiConfirmationRequestDTO;
import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.DishService;
import com.sky.service.SensitiveWordService;
import com.sky.service.UserAiChatService;
import com.sky.service.ai.AgentServiceHttp;
import com.sky.service.ai.user.UserAiSessionManager;
import com.sky.vo.AiReviewWriteVO;
import com.sky.vo.AiSessionVO;
import com.sky.vo.AiRecommendItemVO;
import com.sky.vo.DishVO;
import com.sky.vo.SensitiveWordCheckVO;
import com.sky.vo.UserAiChatResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class UserAiChatServiceImpl implements UserAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private final AgentServiceHttp agentServiceHttp;
    private final UserAiSessionManager sessionManager;
    private final DishService dishService;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final SensitiveWordService sensitiveWordService;

    public UserAiChatServiceImpl(AgentServiceHttp agentServiceHttp,
                                 UserAiSessionManager sessionManager,
                                 DishService dishService,
                                 OrderMapper orderMapper,
                                 OrderDetailMapper orderDetailMapper,
                                 SensitiveWordService sensitiveWordService) {
        this.agentServiceHttp = agentServiceHttp;
        this.sessionManager = sessionManager;
        this.dishService = dishService;
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.sensitiveWordService = sensitiveWordService;
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
            AgentServiceHttp.Result result = callCompletion(session, message);
            String content = result.getAnswer();
            sessionManager.appendMessage(session, "assistant", content);
            sessionManager.touchSession(session, content);
            UserAiChatResponseVO response = new UserAiChatResponseVO();
            response.setSessionId(session.getId());
            response.setContent(content);
            response.setStatus(result.getStatus());
            response.setConfirmation(result.getConfirmation());
            response.setTraceId(result.getTraceId());
            return response;
        } catch (Exception ex) {
            sessionManager.rollbackLastUserMessage(session, message);
            log.error("用户端 AI 对话失败：{}", ex.getMessage(), ex);
            throw new IllegalStateException("智能客服暂时不可用，请稍后再试");
        } finally {
            BaseContext.removeCurrentId();
        }
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
        if ("edit".equals(decision) && request.getEditedArguments() == null) {
            throw new IllegalArgumentException("修改确认必须提供新参数");
        }
        Long userId = currentUserId();
        UserAiSessionManager.ChatSession session = sessionManager.getOwnedSession(
                userId, request.getSessionId());
        AgentServiceHttp.Result result = agentServiceHttp.resumeUser(
                UUID.randomUUID().toString(), userId, session.getId(),
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

    @Override
    public List<AiRecommendItemVO> recommend(AiRecommendRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getRequirement())) {
            throw new IllegalArgumentException("推荐需求不能为空");
        }
        Map<String,Object> response = agentServiceHttp.recommend(
                UUID.randomUUID().toString(), currentUserId(), request.getRequirement(),
                request.getBudget(), request.getPeopleCount());
        Object raw = response.get("items");
        if (!(raw instanceof List)) return Collections.emptyList();
        List<AiRecommendItemVO> result = new ArrayList<>();
        for (Object value : (List<?>) raw) {
            if (!(value instanceof Map)) continue;
            Map<?,?> item=(Map<?,?>)value;
            AiRecommendItemVO vo=new AiRecommendItemVO();
            vo.setDishId(Long.valueOf(String.valueOf(item.get("id"))));
            vo.setName(String.valueOf(item.get("name")));
            vo.setImage(item.get("image")==null?null:String.valueOf(item.get("image")));
            vo.setPrice(item.get("price")==null?null:new java.math.BigDecimal(String.valueOf(item.get("price"))));
            vo.setReason(String.valueOf(item.get("reason")));
            result.add(vo);
        }
        return result;
    }

    @Override
    public AiReviewWriteVO writeReview(AiReviewWriteRequestDTO request) {
        if (request == null || request.getOrderId() == null || request.getDishId() == null)
            throw new IllegalArgumentException("订单和菜品编号不能为空");
        String highlights=StringUtils.hasText(request.getDraft())?request.getDraft():request.getKeywords();
        Map<String,Object> response=agentServiceHttp.reviewDraft(
                UUID.randomUUID().toString(),currentUserId(),request.getOrderId(),request.getDishId(),
                request.getRating(),highlights,request.getStyle());
        AiReviewWriteVO vo = new AiReviewWriteVO();
        vo.setContent(String.valueOf(response.get("content")));
        vo.setFlagged(Boolean.TRUE.equals(response.get("flagged")));
        vo.setSensitiveWords(Collections.emptyList());
        return vo;
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
            send(emitter, "meta", mapOf("sessionId", session.getId(), "provider", "agent-api"), closed);
            AgentServiceHttp.Result result = agentServiceHttp.userStream(
                    UUID.randomUUID().toString(), userId, session.getId(), message,
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
                    "status", result.getStatus(),
                    "durationMs", System.currentTimeMillis() - startedAt
            ), closed);
            emitter.complete();
        } catch (Exception ex) {
            log.error("用户端 AI 工具调用失败：{}", ex.getMessage(), ex);
            sessionManager.rollbackLastUserMessage(session, message);
            try {
                send(emitter, "error", mapOf("code", "AI_SERVICE_UNAVAILABLE", "message", "智能客服暂时不可用"), closed);
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(ex);
            }
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    private AgentServiceHttp.Result callCompletion(UserAiSessionManager.ChatSession session,
                                                   String latestMessage) {
        return agentServiceHttp.userChat(
                UUID.randomUUID().toString(), currentUserId(), session.getId(), latestMessage);
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

    private List<AiRecommendItemVO> fallbackRecommend(List<DishVO> dishes) {
        List<AiRecommendItemVO> result = new ArrayList<>();
        int limit = Math.min(dishes.size(), 6);
        for (int i = 0; i < limit; i++) {
            DishVO dish = dishes.get(i);
            AiRecommendItemVO item = new AiRecommendItemVO();
            item.setDishId(dish.getId());
            item.setName(dish.getName());
            item.setImage(dish.getImage());
            item.setPrice(dish.getPrice());
            item.setCategoryName(dish.getCategoryName());
            item.setReason("当前真实可售，适合作为推荐候选。");
            result.add(item);
        }
        return result;
    }

    private String trimTo140(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", "");
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
