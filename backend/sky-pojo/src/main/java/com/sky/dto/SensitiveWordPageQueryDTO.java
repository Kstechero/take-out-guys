package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SensitiveWordPageQueryDTO implements Serializable {
    private Integer page;
    private Integer pageSize;
    private String word;
    private Integer status;
}
