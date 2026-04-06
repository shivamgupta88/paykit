package com.paykit.domain.tenant.dto;

import com.paykit.domain.tenant.TenantStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private TenantStatus status;
    private String contactEmail;
    private Instant createdAt;
}
