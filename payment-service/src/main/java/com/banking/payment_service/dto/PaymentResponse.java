package com.banking.payment_service.dto;

import com.banking.payment_service.entity.enums.PaymentMethod;
import com.banking.payment_service.entity.enums.PaymentOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        BigDecimal amount,
        PaymentOrderStatus status,
        PaymentMethod paymentMethod,
        String paymentLinkId,
        String accountNumber,
        String currency,
        String description,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}