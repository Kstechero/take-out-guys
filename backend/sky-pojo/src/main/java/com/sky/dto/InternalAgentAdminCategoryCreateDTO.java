package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/** Strict payload for a confirmed Agent category creation. */
@Data
public class InternalAgentAdminCategoryCreateDTO implements Serializable {
    private String name;
    private Integer type;
    private Integer sort;
    @JsonProperty("audit_reason")
    private String auditReason;
}
