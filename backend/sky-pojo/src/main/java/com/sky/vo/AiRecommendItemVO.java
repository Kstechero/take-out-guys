package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AiRecommendItemVO implements Serializable {
    private Long dishId;
    private String name;
    private String image;
    private BigDecimal price;
    private String categoryName;
    private String reason;
}
