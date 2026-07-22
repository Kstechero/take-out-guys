package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DishReviewLike implements Serializable {
    private Long id;
    private Long reviewId;
    private Long userId;
    private LocalDateTime createTime;
}
