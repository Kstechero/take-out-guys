package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sky.entity.DishFlavor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Strict payload for confirmed Agent dish creation or optimistic updates. */
@Data
public class InternalAgentAdminDishMutationDTO implements Serializable {
    private String name;
    @JsonProperty("category_id")
    private Long categoryId;
    private BigDecimal price;
    private String image;
    private String description;
    private Integer status;
    private List<DishFlavor> flavors;
    @JsonProperty("expected_updated_at")
    private LocalDateTime expectedUpdatedAt;
    @JsonProperty("audit_reason")
    private String auditReason;
}
