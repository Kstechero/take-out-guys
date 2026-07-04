package com.sky.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.properties.AiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes the OpenAI-compatible tool-calling loop against the configured model. */
@Component
public class AiToolCallingClient {

    private static final int MAX_TOOL_ROUNDS = 5;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AiToolCallingClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String complete(String systemPrompt, List<Map<String, Object>> history,
                           List<Map<String, Object>> tools, ToolExecutor executor) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(mapOf("role", "system", "content", systemPrompt));
        messages.addAll(history);

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            JsonNode message = request(messages, tools);
            JsonNode toolCalls = message.path("tool_calls");
            if (!toolCalls.isArray() || toolCalls.size() == 0) {
                String content = message.path("content").asText("").trim();
                if (!StringUtils.hasText(content)) {
                    throw new IllegalStateException("Model returned neither content nor tool calls");
                }
                return content;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> assistantMessage = objectMapper.convertValue(message, Map.class);
            messages.add(assistantMessage);
            for (JsonNode toolCall : toolCalls) {
                String id = toolCall.path("id").asText();
                String name = toolCall.path("function").path("name").asText();
                String rawArguments = toolCall.path("function").path("arguments").asText("{}");
                JsonNode arguments;
                try {
                    arguments = objectMapper.readTree(rawArguments);
                } catch (Exception ex) {
                    arguments = objectMapper.createObjectNode();
                }
                String result;
                try {
                    result = executor.execute(name, arguments);
                } catch (Exception ex) {
                    result = objectMapper.writeValueAsString(mapOf("ok", false, "error", ex.getMessage()));
                }
                messages.add(mapOf("role", "tool", "tool_call_id", id, "name", name, "content", result));
            }
        }
        throw new IllegalStateException("Tool calling exceeded " + MAX_TOOL_ROUNDS + " rounds");
    }

    private JsonNode request(List<Map<String, Object>> messages, List<Map<String, Object>> tools) throws Exception {
        validateConfiguration();
        HttpURLConnection connection = (HttpURLConnection) new URL(trimTrailingSlash(properties.getBaseUrl()) + "/chat/completions").openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(properties.getConnectTimeout());
            connection.setReadTimeout(properties.getReadTimeout());
            connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");

            Map<String, Object> body = mapOf(
                    "model", properties.getModel(),
                    "stream", false,
                    "temperature", properties.getTemperature(),
                    "max_tokens", properties.getMaxTokens(),
                    "messages", messages,
                    "tools", tools,
                    "tool_choice", "auto"
            );
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("AI provider returned HTTP " + status + ": " + read(connection.getErrorStream()));
            }
            JsonNode choices = objectMapper.readTree(connection.getInputStream()).path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                throw new IllegalStateException("AI provider returned no choices");
            }
            return choices.get(0).path("message");
        } finally {
            connection.disconnect();
        }
    }

    public static Map<String, Object> tool(String name, String description, Map<String, Object> properties,
                                           String... required) {
        Map<String, Object> parameters = mapOf("type", "object", "properties", properties,
                "additionalProperties", false);
        if (required.length > 0) {
            parameters.put("required", required);
        }
        return mapOf("type", "function", "function",
                mapOf("name", name, "description", description, "parameters", parameters));
    }

    public static Map<String, Object> stringProperty(String description) {
        return mapOf("type", "string", "description", description);
    }

    public static Map<String, Object> booleanProperty(String description) {
        return mapOf("type", "boolean", "description", description);
    }

    public static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getBaseUrl()) || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())) {
            throw new IllegalStateException("AI provider configuration is incomplete");
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String read(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }
        byte[] buffer = new byte[2048];
        int length = input.read(buffer);
        return length < 0 ? "" : new String(buffer, 0, length, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String name, JsonNode arguments) throws Exception;
    }
}
