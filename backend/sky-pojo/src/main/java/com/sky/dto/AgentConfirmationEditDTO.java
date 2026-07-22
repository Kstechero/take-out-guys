package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** Typed fields that the user may edit while resolving an Agent confirmation. */
@Data
public class AgentConfirmationEditDTO implements Serializable {
    @JsonProperty("dish_id")
    private Long dishId;
    @JsonProperty("setmeal_id")
    private Long setmealId;
    @JsonProperty("cart_item_id")
    private Long cartItemId;
    private String flavor;
    private Integer quantity;
    @JsonProperty("expected_unit_amount")
    private BigDecimal expectedUnitAmount;
    @JsonProperty("coupon_id")
    private Long couponId;
    @JsonProperty("order_id")
    private Long orderId;
    private String action;
    private String status;
    @JsonProperty("audit_reason")
    private String auditReason;
}
