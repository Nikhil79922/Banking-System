package com.banking.transaction_service.dto;

import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(

        String id,

        String senderAccountNumber,

        String receiverAccountNumber,

        BigDecimal amount,

        TransactionType type,

        TransactionStatus status,

        String description,

        String failureReason,

        String referenceNumber,

        LocalDateTime createdAt,

        LocalDateTime completedAt
) {
}