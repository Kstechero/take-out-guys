package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/** Strict payload for a confirmed Agent order transition. */
@Data
public class InternalAgentAdminOrderActionDTO implements Serializable {
    private String action;
    @JsonProperty("expected_status")
    private Integer expectedStatus;
    @JsonProperty("audit_reason")
    private String auditReason;
}
