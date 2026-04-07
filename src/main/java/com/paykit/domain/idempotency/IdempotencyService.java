package com.paykit.domain.idempotency;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyService {

    Optional<IdempotencyLog> check(UUID tenantId, String idempotencyKey);

    IdempotencyLog save(UUID tenantId, String idempotencyKey, String requestPath, String requestHash,
                        int responseStatus, String responseBody);

    void cleanup();
}
