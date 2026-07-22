package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerServiceSessionPageQueryDTO implements Serializable {
    private Integer page;
    private Integer pageSize;
    private Integer status;
    private String keyword;
}
