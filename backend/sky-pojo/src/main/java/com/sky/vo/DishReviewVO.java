package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DishReviewVO implements Serializable {
    private Long id;
    private Long orderId;
    private Long dishId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String content;
    private String imagesJson;
    private List<String> images;
    private Integer likeCount;
    private Boolean liked;
    private LocalDateTime createTime;
}
