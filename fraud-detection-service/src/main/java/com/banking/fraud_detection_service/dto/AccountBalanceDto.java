package com.banking.fraud_detection_service.dto;

import java.math.BigDecimal;

public record AccountBalanceDto(
        String accountNumber,
        BigDecimal balance
) {}
