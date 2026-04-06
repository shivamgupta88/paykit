package com.paykit.domain.tenant;

import com.paykit.domain.tenant.dto.CreateTenantRequest;
import com.paykit.domain.tenant.dto.TenantResponse;

import java.util.UUID;

public interface TenantService {

    TenantResponse create(CreateTenantRequest request);

    TenantResponse getBySlug(String slug);

    TenantResponse getById(UUID id);
}
