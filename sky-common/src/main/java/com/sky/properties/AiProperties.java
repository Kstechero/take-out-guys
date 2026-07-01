package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
}
