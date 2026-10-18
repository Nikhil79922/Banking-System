package com.banking.account_service.mapper;

import com.banking.account_service.dto.AccountBalanceResponse;
import com.banking.account_service.dto.CreateAccountRequest;
import com.banking.account_service.dto.UpdateAccountRequest;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.entity.Account;
import com.banking.account_service.projection.AccountBalanceProjection;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toEntity(CreateAccountRequest request) {

        Account account = new Account();

        account.setAccountHolderName(request.accountHolderName());
        account.setEmail(request.email());
        account.setPhone(request.phone());
        account.setAccountType(request.accountType());
        account.setBalance(request.initialDeposit());

        return account;
    }

    public AccountResponse toResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getEmail(),
                account.getPhone(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getDailyTransactionLimit(),
                account.getCreatedAt()
//                account.getUpdatedAt()
        );
    }


    public AccountBalanceResponse toBalanceResponse(AccountBalanceProjection projection) {
        return new AccountBalanceResponse(
                projection.getAccountNumber(),
                projection.getBalance()
        );
    }

    public void updateEntity(
            Account account,
            UpdateAccountRequest request
    ) {

        if (request.accountHolderName() != null) {
            account.setAccountHolderName(
                    request.accountHolderName()
            );
        }

        if (request.email() != null) {
            account.setEmail(
                    request.email()
            );
        }

        if (request.phone() != null) {
            account.setPhone(
                    request.phone()
            );
        }

        if (request.dailyTransactionLimit() != null) {
            account.setDailyTransactionLimit(
                    request.dailyTransactionLimit()
            );
        }
    }
}