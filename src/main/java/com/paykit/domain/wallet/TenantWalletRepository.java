package com.paykit.domain.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface TenantWalletRepository extends JpaRepository<TenantWallet, UUID> {

    Optional<TenantWallet> findByTenantId(UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM TenantWallet w WHERE w.tenantId = :tenantId")
    Optional<TenantWallet> findByTenantIdForUpdate(UUID tenantId);
}
