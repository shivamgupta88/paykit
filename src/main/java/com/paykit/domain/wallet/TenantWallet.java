package com.paykit.domain.wallet;

import com.paykit.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tenant_wallets")
@Getter
@Setter
public class TenantWallet extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;
}
