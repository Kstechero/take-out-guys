package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Strict payload for a confirmed Agent coupon creation. */
@Data
public class InternalAgentAdminCouponCreateDTO implements Serializable {
    private String name;
    private Integer type;
    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;
    @JsonProperty("minimum_amount")
    private BigDecimal minimumAmount;
    @JsonProperty("total_count")
    private Integer totalCount;
    @JsonProperty("per_user_limit")
    private Integer perUserLimit;
    @JsonProperty("valid_from")
    private LocalDateTime validFrom;
    @JsonProperty("valid_until")
    private LocalDateTime validUntil;
    private Integer status;
    private String description;
    @JsonProperty("audit_reason")
    private String auditReason;
}
