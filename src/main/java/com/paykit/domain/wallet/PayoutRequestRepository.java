package com.paykit.domain.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    Page<PayoutRequest> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
