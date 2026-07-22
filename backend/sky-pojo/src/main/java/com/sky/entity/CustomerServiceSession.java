package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomerServiceSession implements Serializable {
    public static final Integer OPEN = 1;
    public static final Integer CLOSED = 2;

    private Long id;
    private Long userId;
    private String source;
    private Integer status;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime closedTime;
}
