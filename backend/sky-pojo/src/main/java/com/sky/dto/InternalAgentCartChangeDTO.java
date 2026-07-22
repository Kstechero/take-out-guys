package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** Strict request contract for confirmed Agent cart mutations. */
@Data
public class InternalAgentCartChangeDTO implements Serializable {
    private String action;
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
}
