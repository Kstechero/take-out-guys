package com.sky.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO implements Serializable {
    private Long id;
    private Long userCouponId;
    private Long couponId;
    private String name;
    private Integer type;
    private BigDecimal discountAmount;
    private BigDecimal minimumAmount;
    private Integer status;
    private String description;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
    private Long orderId;
    private LocalDateTime expireTime;
}
