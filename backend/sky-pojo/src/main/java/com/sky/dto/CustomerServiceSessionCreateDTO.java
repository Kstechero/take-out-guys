package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerServiceSessionCreateDTO implements Serializable {
    private String source;
    private String initialMessage;
}
