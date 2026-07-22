package com.sky.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponDTO implements Serializable {
    private String name;
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal minimumAmount;
    private Integer totalCount;
    private Integer perUserLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer status;
    private String description;
}
