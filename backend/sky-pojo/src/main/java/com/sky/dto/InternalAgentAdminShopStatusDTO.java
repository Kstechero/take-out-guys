package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/** Strict payload for a confirmed Agent shop-status change. */
@Data
public class InternalAgentAdminShopStatusDTO implements Serializable {
    private String status;
    @JsonProperty("expected_status")
    private String expectedStatus;
    @JsonProperty("audit_reason")
    private String auditReason;
}
