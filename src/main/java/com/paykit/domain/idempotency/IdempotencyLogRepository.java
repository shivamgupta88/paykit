package com.paykit.domain.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyLogRepository extends JpaRepository<IdempotencyLog, UUID> {

    Optional<IdempotencyLog> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    void deleteByExpiresAtBefore(Instant instant);
}
