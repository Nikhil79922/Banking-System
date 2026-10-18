package com.banking.account_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateAccountRequest(

        @Size(max = 100, message = "Account holder name must not exceed 100 characters")
        String accountHolderName,

        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Invalid Indian phone number"
        )
        String phone,

        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Daily transaction limit must be greater than 0"
        )
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Daily transaction limit must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal dailyTransactionLimit
) {
}
