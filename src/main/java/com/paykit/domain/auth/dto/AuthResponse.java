package com.paykit.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private UUID userId;
    private UUID tenantId;
}
