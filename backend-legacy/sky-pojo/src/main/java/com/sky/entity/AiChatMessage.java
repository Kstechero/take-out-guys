package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiChatMessage implements Serializable {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
