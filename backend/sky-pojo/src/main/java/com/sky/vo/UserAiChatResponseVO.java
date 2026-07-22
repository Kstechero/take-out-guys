package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class UserAiChatResponseVO implements Serializable {
    private Long sessionId;
    private String content;
    private String status;
    private Map<String, Object> confirmation;
    private String traceId;
}
