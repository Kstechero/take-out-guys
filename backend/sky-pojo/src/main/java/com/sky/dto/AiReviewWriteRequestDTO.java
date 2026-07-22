package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiReviewWriteRequestDTO implements Serializable {
    private Long orderId;
    private Long dishId;
    private Integer rating;
    private String keywords;
    private String draft;
    private String style;
}
