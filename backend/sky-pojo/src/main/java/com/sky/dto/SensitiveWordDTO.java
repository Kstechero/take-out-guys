package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensitiveWordDTO implements Serializable {
    private String word;
    private Integer level;
    private String replacement;
    private Integer status;
}
