package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AiRecommendRequestDTO implements Serializable {
    private String requirement;
    private BigDecimal budget;
    private Integer peopleCount;
}
