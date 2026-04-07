package com.paykit.domain.idempotency;

import com.paykit.common.AppConstants;
import com.paykit.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyLogRepository idempotencyLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyLog> check(UUID tenantId, String idempotencyKey) {
        return idempotencyLogRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
    }

    @Override
    @Transactional
    public IdempotencyLog save(UUID tenantId, String idempotencyKey, String requestPath, String requestHash,
                               int responseStatus, String responseBody) {
        TenantContext.set(tenantId);
        IdempotencyLog log = new IdempotencyLog();
        log.setIdempotencyKey(idempotencyKey);
        log.setRequestPath(requestPath);
        log.setRequestHash(requestHash);
        log.setResponseStatus(responseStatus);
        log.setResponseBody(responseBody);
        log.setExpiresAt(Instant.now().plus(AppConstants.IDEMPOTENCY_TTL_HOURS, ChronoUnit.HOURS));
        return idempotencyLogRepository.save(log);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 3600000)
    public void cleanup() {
        idempotencyLogRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
