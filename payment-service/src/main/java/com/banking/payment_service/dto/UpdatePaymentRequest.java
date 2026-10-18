package com.banking.payment_service.dto;

import jakarta.validation.constraints.Size;

public record UpdatePaymentRequest(

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description
) {
}