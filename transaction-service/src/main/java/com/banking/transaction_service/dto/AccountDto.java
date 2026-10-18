package com.banking.transaction_service.dto;


import com.banking.transaction_service.entity.enums.AccountStatus;
import com.banking.transaction_service.entity.enums.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDto(

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