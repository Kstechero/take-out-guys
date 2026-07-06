package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomerServiceMessageVO implements Serializable {
    private Long id;
    private Long sessionId;
    private String senderType;
    private Long senderId;
    private String senderName;
    private String messageType;
    private String content;
    private Integer flagged;
    private Integer readStatus;
    private LocalDateTime createTime;
}
