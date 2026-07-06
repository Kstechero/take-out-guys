package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CustomerServiceSessionVO implements Serializable {
    private Long id;
    private Long userId;
    private String userName;
    private String source;
    private Integer status;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
