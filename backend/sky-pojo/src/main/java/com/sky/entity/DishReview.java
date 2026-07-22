package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DishReview implements Serializable {
    private Long id;
    private Long userId;
    private Long orderId;
    private Long dishId;
    private Integer rating;
    private String content;
    private String images;
    private Integer likeCount;
    private Integer status;
    private Integer aiGenerated;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
