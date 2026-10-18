package com.banking.account_service.dto;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        String accountNumber,
        BigDecimal balance
) {}
