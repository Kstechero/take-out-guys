package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomerServiceMessage implements Serializable {
    private Long id;
    private Long sessionId;
    private String senderType;
    private Long senderId;
    private String messageType;
    private String content;
    private Integer flagged;
    private LocalDateTime createTime;
}
