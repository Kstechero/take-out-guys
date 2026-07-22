package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerServiceMessageDTO implements Serializable {
    private Long sessionId;
    private String content;
    private String messageType;
}
