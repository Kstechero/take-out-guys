package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminReviewPageVO implements Serializable {
    private Long id;
    private Long userId;
    private String userName;
    private Long orderId;
    private String orderNumber;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private Integer rating;
    private String content;
    private String imagesJson;
    private List<String> images;
    private Integer likeCount;
    private Integer status;
    private Integer aiGenerated;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
