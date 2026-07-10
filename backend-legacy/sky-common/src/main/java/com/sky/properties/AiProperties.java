package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** GX10 vLLM（OpenAI-compatible）连接配置。 */
@Component
@ConfigurationProperties(prefix = "sky.ai")
@Data
public class AiProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    private int connectTimeout = 10000;
    private int readTimeout = 120000;
    private int maxTokens = 2048;
    private double temperature = 0.3D;
    private RagProperties rag = new RagProperties();
    private McpProperties mcp = new McpProperties();

    @Data
    public static class RagProperties {
        private boolean enabled = true;
        private int topK = 4;
        private int maxSegmentLength = 600;
        private int maxOverlap = 80;
        private int maxSegmentsPerSource = 120;
        private List<String> sourcePaths = new ArrayList<>();
    }

    @Data
    public static class McpProperties {
        private boolean enabled = true;
        private String serverName = "takeout-guys-enterprise";
        private String serverVersion = "1.0.0";
    }
}
