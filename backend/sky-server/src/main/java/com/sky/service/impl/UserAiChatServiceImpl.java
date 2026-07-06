package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sky.context.BaseContext;
import com.sky.dto.AiRecommendRequestDTO;
import com.sky.dto.AiReviewWriteRequestDTO;
import com.sky.dto.UserAiChatRequestDTO;
import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.properties.AiProperties;
import com.sky.service.DishService;
import com.sky.service.SensitiveWordService;
import com.sky.service.UserAiChatService;
import com.sky.service.ai.AiToolCallingClient;
import com.sky.service.ai.knowledge.OperationsKnowledgeService;
import com.sky.service.ai.user.UserAiSessionManager;
import com.sky.service.ai.user.UserAiToolExecutor;
import com.sky.service.ai.user.UserAiToolRegistry;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class UserAiChatServiceImpl implements UserAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private static final String TOOL_POLICY = "\nFor current business data or business actions, you must use an available tool. "
            + "Never guess tool results. Only call mutation tools after explicit user confirmation, and only when the tool arguments include confirmed=true.";
    private static final String SYSTEM_PROMPT =
            "You are Xiaonuan, the user-side intelligent service assistant for Takeout Guys. "
                    + "Always respond in concise, friendly Chinese. "
                    + "For current business data or actions such as shop status, menu, orders, cart, coupons, and addresses, always use tools instead of guessing. "
                    + "Tool outputs are authoritative structured JSON from backend services. "
                    + "For delivery range, after-sales rules, coupon rules, dine-in pickup, platform usage, and other service-process questions, prefer service knowledge tools before answering. "
                    + "For any user-data mutation such as receiving coupons, editing addresses, clearing cart, cancelling orders, reordering, or sending reminders, ask for explicit confirmation first and only then call the tool with confirmed=true. "
                    + "If the user asks for capabilities that are not yet connected, clearly state the current product boundary.";

    private final AiProperties properties;
    private final AiToolCallingClient toolCallingClient;
    private final UserAiToolRegistry toolRegistry;
    private final UserAiToolExecutor toolExecutor;
    private final UserAiSessionManager sessionManager;
    private final OperationsKnowledgeService knowledgeService;
    private final DishService dishService;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final SensitiveWordService sensitiveWordService;

    public UserAiChatServiceImpl(AiProperties properties,
                                 AiToolCallingClient toolCallingClient,
                                 UserAiToolRegistry toolRegistry,
                                 UserAiToolExecutor toolExecutor,
                                 UserAiSessionManager sessionManager,
                                 OperationsKnowledgeService knowledgeService,
                                 DishService dishService,
                                 OrderMapper orderMapper,
                                 OrderDetailMapper orderDetailMapper,
                                 SensitiveWordService sensitiveWordService) {
        this.properties = properties;
        this.toolCallingClient = toolCallingClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.sessionManager = sessionManager;
        this.knowledgeService = knowledgeService;
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
            String content = callCompletion(session, message);
            sessionManager.appendMessage(session, "assistant", content);
            sessionManager.touchSession(session, content);
            UserAiChatResponseVO response = new UserAiChatResponseVO();
            response.setSessionId(session.getId());
            response.setContent(content);
            return response;
        } catch (Exception ex) {
            sessionManager.rollbackLastUserMessage(session, message);
            log.error("User AI chat failed: {}", ex.getMessage(), ex);
            throw new IllegalStateException("智能客服暂时不可用，请稍后再试");
        } finally {
            BaseContext.removeCurrentId();
        }
    }

    @Override
    public List<AiRecommendItemVO> recommend(AiRecommendRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getRequirement())) {
            throw new IllegalArgumentException("推荐需求不能为空");
        }
        Dish probe = new Dish();
        probe.setStatus(1);
        List<DishVO> dishes = dishService.listWithFlavor(probe);
        if (dishes == null || dishes.isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder menu = new StringBuilder();
        int limit = Math.min(dishes.size(), 30);
        for (int i = 0; i < limit; i++) {
            DishVO dish = dishes.get(i);
            menu.append(i + 1)
                    .append(". id=").append(dish.getId())
                    .append(", name=").append(dish.getName())
                    .append(", price=").append(dish.getPrice())
                    .append(", category=").append(dish.getCategoryName())
                    .append(", description=").append(dish.getDescription() == null ? "" : dish.getDescription())
                    .append('\n');
        }
        String systemPrompt = "你是外卖菜品推荐助手。只允许从候选菜品中挑选，最多返回6个推荐项。"
                + "输出严格 JSON 数组，每项字段必须为 dishId,name,image,price,categoryName,reason。"
                + "不要输出 markdown，不要输出额外说明。";
        String userPrompt = "用户需求：" + request.getRequirement()
                + "\n预算：" + request.getBudget()
                + "\n人数：" + request.getPeopleCount()
                + "\n候选菜品：\n" + menu;
        try {
            String content = toolCallingClient.complete(systemPrompt,
                    Collections.singletonList(mapOf("role", "user", "content", userPrompt)),
                    Collections.emptyList(),
                    (name, args) -> "{}");
            List<AiRecommendItemVO> items = JSON.parseObject(content, new TypeReference<List<AiRecommendItemVO>>() {});
            if (items == null || items.isEmpty()) {
                return fallbackRecommend(dishes);
            }
            hydrateRecommendFields(items, dishes);
            return items;
        } catch (Exception ex) {
            log.warn("AI recommend fallback triggered: {}", ex.getMessage());
            return fallbackRecommend(dishes);
        }
    }

    @Override
    public AiReviewWriteVO writeReview(AiReviewWriteRequestDTO request) {
        if (request == null || request.getOrderId() == null) {
            throw new IllegalArgumentException("订单编号不能为空");
        }
        Long userId = currentUserId();
        Orders order = orderMapper.getById(request.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        List<OrderDetail> details = orderDetailMapper.getByOrderId(request.getOrderId());
        StringBuilder dishes = new StringBuilder();
        for (OrderDetail detail : details) {
            if (request.getDishId() == null || request.getDishId().equals(detail.getDishId())) {
                dishes.append(detail.getName()).append(" x").append(detail.getNumber()).append("；");
            }
        }
        if (dishes.length() == 0) {
            throw new IllegalArgumentException("未找到可评价菜品");
        }

        String systemPrompt = "你是外卖评价帮写助手。请使用中文生成1条自然、真实、口语化的用户评价，长度不超过140字。"
                + "不要编造未购买菜品，不要输出解释。";
        String userPrompt = "订单菜品：" + dishes
                + "\n评分：" + request.getRating()
                + "\n关键词：" + request.getKeywords()
                + "\n用户草稿：" + request.getDraft()
                + "\n风格：" + request.getStyle();
        try {
            String content = toolCallingClient.complete(systemPrompt,
                    Collections.singletonList(mapOf("role", "user", "content", userPrompt)),
                    Collections.emptyList(),
                    (name, args) -> "{}");
            String normalized = trimTo140(content);
            SensitiveWordCheckVO moderation = sensitiveWordService.scanText(normalized);
            AiReviewWriteVO vo = new AiReviewWriteVO();
            vo.setContent(moderation.getContent());
            vo.setFlagged(moderation.getHit());
            vo.setSensitiveWords(moderation.getWords());
            return vo;
        } catch (Exception ex) {
            log.error("AI review writing failed: {}", ex.getMessage(), ex);
            throw new IllegalStateException("AI 评价帮写暂时不可用，请稍后再试");
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
            String answer = callCompletion(session, message);
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

    private String callCompletion(UserAiSessionManager.ChatSession session, String latestMessage) throws Exception {
        String knowledgeContext = knowledgeService.buildGroundingContext(latestMessage, 3);
        String systemPrompt = SYSTEM_PROMPT + TOOL_POLICY;
        if (StringUtils.hasText(knowledgeContext)) {
            systemPrompt = systemPrompt + "\n\n" + knowledgeContext;
        }
        return toolCallingClient.complete(
                systemPrompt,
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

    private void hydrateRecommendFields(List<AiRecommendItemVO> items, List<DishVO> dishes) {
        Map<Long, DishVO> dishMap = new LinkedHashMap<>();
        for (DishVO dish : dishes) {
            dishMap.put(dish.getId(), dish);
        }
        for (AiRecommendItemVO item : items) {
            DishVO dish = dishMap.get(item.getDishId());
            if (dish == null) {
                continue;
            }
            if (!StringUtils.hasText(item.getName())) {
                item.setName(dish.getName());
            }
            if (!StringUtils.hasText(item.getImage())) {
                item.setImage(dish.getImage());
            }
            if (item.getPrice() == null) {
                item.setPrice(dish.getPrice());
            }
            if (!StringUtils.hasText(item.getCategoryName())) {
                item.setCategoryName(dish.getCategoryName());
            }
        }
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
