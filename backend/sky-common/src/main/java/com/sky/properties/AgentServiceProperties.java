package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.agent-service")
@Data
public class AgentServiceProperties {
    private String baseUrl = "http://127.0.0.1:8000";
    private String authToken;
    private String internalAuthToken;
    private boolean internalWritesEnabled;
    private int connectTimeout = 3000;
    private int readTimeout = 120000;
}
