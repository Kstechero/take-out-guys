package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiChatSession implements Serializable {
    private Long id;
    private String scope;
    private Long ownerId;
    private String title;
    private String lastMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
