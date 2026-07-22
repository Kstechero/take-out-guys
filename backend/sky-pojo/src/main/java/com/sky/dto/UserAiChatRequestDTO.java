package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAiChatRequestDTO implements Serializable {
    private Long sessionId;
    private String message;
}
