package com.banking.transaction_service.dto;

import com.banking.transaction_service.entity.enums.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTransactionRequest(

        @NotBlank(message = "Sender account number is required")
        String senderAccountNumber,

        @NotBlank(message = "Receiver account number is required")
        String receiverAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}