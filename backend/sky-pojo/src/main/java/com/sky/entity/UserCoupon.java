package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon implements Serializable {
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
    private Long orderId;
    private LocalDateTime expireTime;
}
