package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAiConfirmationRequestDTO implements Serializable {
    private Long sessionId;
    private String confirmationToken;
    private String decision;
    private AgentConfirmationEditDTO editedArguments;
}
