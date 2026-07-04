package com.sky.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAiChatResponseVO implements Serializable {
    private Long sessionId;
    private String content;
}
