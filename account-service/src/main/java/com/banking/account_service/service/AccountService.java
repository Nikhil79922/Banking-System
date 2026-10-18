package com.banking.account_service.service;

import com.banking.account_service.dto.AccountBalanceResponse;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.dto.CreateAccountRequest;

import java.math.BigDecimal;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(String accountNumber);

    AccountBalanceResponse getBalance(String accountNumber);

    AccountResponse blockAccount(String accountNumber);

    AccountResponse deductBalance(
            String accountNumber,
            BigDecimal amount
    );

    AccountResponse creditBalance(
            String accountNumber,
            BigDecimal amount
    );
}