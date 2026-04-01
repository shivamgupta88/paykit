package com.paykit.tenant;

import com.paykit.common.BaseEntity;
import com.paykit.exception.TenantResolutionException;
import jakarta.persistence.PrePersist;

import java.util.UUID;

public class TenantEntityListener {

    @PrePersist
    public void setTenantOnCreate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            if (baseEntity.getTenantId() != null) {
                return;
            }
            UUID tenantId = TenantContext.get();
            if (tenantId == null) {
                throw new TenantResolutionException("Tenant context not set during entity persist");
            }
            baseEntity.setTenantId(tenantId);
        }
    }
}
