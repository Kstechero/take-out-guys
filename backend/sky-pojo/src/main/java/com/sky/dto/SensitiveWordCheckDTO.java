package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensitiveWordCheckDTO implements Serializable {
    private String content;
}
