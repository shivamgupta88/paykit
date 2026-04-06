package com.paykit.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LoginRequest {

    @NotNull
    private UUID tenantId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
