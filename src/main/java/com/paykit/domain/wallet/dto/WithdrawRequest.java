package com.paykit.domain.wallet.dto;

import com.paykit.domain.wallet.PayoutRequest.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WithdrawRequest {

    @NotNull
    @DecimalMin(value = "1.0")
    private BigDecimal amount;

    @NotNull
    private AccountType accountType;

    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String upiId;
}
