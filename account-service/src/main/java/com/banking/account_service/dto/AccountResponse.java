package com.banking.account_service.dto;

import com.banking.account_service.entity.enums.AccountStatus;
import com.banking.account_service.entity.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(

        String id,

        String accountNumber,

        String accountHolderName,

        String email,

        String phone,

        AccountType accountType,

        AccountStatus status,

        BigDecimal balance,

        BigDecimal dailyTransactionLimit,

        LocalDateTime createdAt

//        LocalDateTime updatedAt
) {
}