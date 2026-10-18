package com.banking.account_service.dto;

import com.banking.account_service.entity.enums.AccountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "Account holder name is required")
        @Size(max = 100, message = "Account holder name must not exceed 100 characters")
        String accountHolderName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Invalid Indian phone number"
        )
        String phone,

        @NotNull(message = "Account type is required")
        AccountType accountType,

        @NotNull(message = "Initial deposit is required")
        @Positive(message = "Initial deposit must be greater than 0")
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Initial deposit must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal initialDeposit
) {
}