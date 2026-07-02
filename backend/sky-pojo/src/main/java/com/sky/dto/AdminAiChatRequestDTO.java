package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/** 管理端运营 Agent 对话请求。 */
@Data
public class AdminAiChatRequestDTO implements Serializable {
    private Long sessionId;
    private String message;
    private Map<String, Object> context;
}
