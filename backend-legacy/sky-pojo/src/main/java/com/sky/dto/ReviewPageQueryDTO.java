package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReviewPageQueryDTO implements Serializable {
    private Integer page;
    private Integer pageSize;
    private Long orderId;
    private Long dishId;
}
