package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReviewSubmitDTO implements Serializable {
    private Long orderId;
    private Long dishId;
    private Integer rating;
    private String content;
    private List<String> images;
}
