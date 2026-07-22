package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon implements Serializable {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal minimumAmount;
    private Integer totalCount;
    private Integer remainingCount;
    private Integer perUserLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
}
