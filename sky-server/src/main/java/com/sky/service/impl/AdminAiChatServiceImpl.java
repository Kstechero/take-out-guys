package com.sky.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.properties.AiProperties;
import com.sky.service.AdminAiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AdminAiChatServiceImpl implements AdminAiChatService {

    private static final long SSE_TIMEOUT = 180000L;
    private static final String SYSTEM_PROMPT =
            "你是 Takeout Guys AI 的管理端运营助手。回答应简洁、专业、使用中文。" +
            "当前尚未提供业务工具调用，因此不得编造订单、营业额、用户、库存或退款数据。" +
            "如果用户询问实时业务数据，请明确说明需要接入业务工具后才能查询。";

    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AdminAiChatServiceImpl(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(AdminAiChatRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("消息不能为空");
        }

        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        final AtomicBoolean closed = new AtomicBoolean(false);
        final AtomicReference<HttpURLConnection> connectionRef = new AtomicReference<>();
        Runnable closeConnection = () -> {
            closed.set(true);
            HttpURLConnection connection = connectionRef.get();
            if (connection != null) connection.disconnect();
        };
        emitter.onCompletion(closeConnection);
        emitter.onTimeout(closeConnection);
        emitter.onError(error -> closeConnection.run());

        CompletableFuture.runAsync(() -> callUpstream(request, emitter, closed, connectionRef));
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
            if (status < 200 || status >= 300) throw new IllegalStateException("GX10 返回 HTTP " + status);
            JsonNode models = objectMapper.readTree(connection.getInputStream()).path("data");
            boolean found = false;
            if (models.isArray()) {
                for (JsonNode model : models) if (properties.getModel().equals(model.path("id").asText())) { found = true; break; }
            }
            result.put("status", found ? "UP" : "DOWN");
            if (!found) result.put("message", "目标模型未在 /models 中找到");
        } catch (Exception ex) {
            result.put("status", "DOWN");
            result.put("message", ex.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
        result.put("latencyMs", System.currentTimeMillis() - startedAt);
        result.put("checkedAt", java.time.LocalDateTime.now().toString());
        return result;
    }

    private void callUpstream(AdminAiChatRequestDTO request, SseEmitter emitter,
                              AtomicBoolean closed, AtomicReference<HttpURLConnection> connectionRef) {
        long startedAt = System.currentTimeMillis();
        HttpURLConnection connection = null;
        try {
            validateConfiguration();
            String endpoint = trimTrailingSlash(properties.getBaseUrl()) + "/chat/completions";
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connectionRef.set(connection);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(properties.getConnectTimeout());
            connection.setReadTimeout(properties.getReadTimeout());
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "text/event-stream");

            byte[] body = buildRequestBody(request).getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String errorBody = readAll(connection.getErrorStream());
                throw new IllegalStateException("GX10 返回 HTTP " + status + ": " + abbreviate(errorBody, 300));
            }

            send(emitter, "meta", mapOf("provider", "vllm", "model", properties.getModel()), closed);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!closed.get() && (line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    JsonNode root = objectMapper.readTree(data);
                    JsonNode choices = root.path("choices");
                    if (!choices.isArray() || choices.size() == 0) continue;
                    JsonNode delta = choices.get(0).path("delta");
                    JsonNode content = delta.get("content");
                    if (content != null && !content.isNull() && StringUtils.hasLength(content.asText())) {
                        send(emitter, "delta", Collections.singletonMap("content", content.asText()), closed);
                    }
                }
            }
            if (!closed.get()) {
                send(emitter, "done", mapOf("model", properties.getModel(), "durationMs", System.currentTimeMillis() - startedAt), closed);
                emitter.complete();
            }
            log.info("管理端AI流式对话完成，model={}, durationMs={}", properties.getModel(), System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            log.error("管理端AI流式对话失败: {}", ex.getMessage());
            if (!closed.get()) {
                try {
                    send(emitter, "error", mapOf("code", "AI_SERVICE_UNAVAILABLE", "message", ex.getMessage()), closed);
                    emitter.complete();
                } catch (Exception ignored) {
                    emitter.completeWithError(ex);
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String buildRequestBody(AdminAiChatRequestDTO request) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("stream", true);
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("messages", Arrays.asList(
                mapOf("role", "system", "content", SYSTEM_PROMPT),
                mapOf("role", "user", "content", request.getMessage().trim())
        ));
        return objectMapper.writeValueAsString(body);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getBaseUrl())) throw new IllegalStateException("未配置 sky.ai.base-url");
        if (!StringUtils.hasText(properties.getApiKey())) throw new IllegalStateException("未配置 GX10_AI_API_KEY");
        if (!StringUtils.hasText(properties.getModel())) throw new IllegalStateException("未配置 sky.ai.model");
    }

    private void send(SseEmitter emitter, String event, Object data, AtomicBoolean closed) throws Exception {
        if (!closed.get()) emitter.send(SseEmitter.event().name(event).data(data));
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }

    private String trimTrailingSlash(String value) { return value.endsWith("/") ? value.substring(0, value.length() - 1) : value; }
    private String abbreviate(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put(String.valueOf(values[i]), values[i + 1]);
        return result;
    }
}
