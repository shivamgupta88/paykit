package com.paykit.domain.tenant;

import com.paykit.domain.tenant.dto.CreateTenantRequest;
import com.paykit.domain.tenant.dto.TenantResponse;
import com.paykit.exception.DuplicateResourceException;
import com.paykit.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Override
    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Tenant slug already exists: " + request.getSlug());
        }

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setStatus(TenantStatus.ACTIVE);

        UUID id = UUID.randomUUID();
        tenant.setId(id);
        tenant.setTenantId(id);

        tenant = tenantRepository.save(tenant);
        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
        return tenantMapper.toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
        return tenantMapper.toResponse(tenant);
    }
}
