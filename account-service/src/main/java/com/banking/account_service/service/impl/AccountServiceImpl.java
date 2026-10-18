package com.banking.account_service.service.impl;

import com.banking.account_service.dto.AccountBalanceResponse;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.dto.CreateAccountRequest;
import com.banking.account_service.entity.Account;
import com.banking.account_service.entity.enums.AccountStatus;
import com.banking.account_service.entity.enums.AccountType;
import com.banking.account_service.mapper.AccountMapper;
import com.banking.account_service.projection.AccountBalanceProjection;
import com.banking.account_service.respository.AccountRepository;
import com.banking.account_service.service.AccountService;
import com.banking.common.exception.ForbiddenException;
import com.banking.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Create a new bank account.
     * @param request account creation details
     * @return created account details
     */
    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {

        log.info("Creating account for email: {}", request.email());

        if (accountRepository.existsByEmail(request.email())) {
            log.warn("Account creation failed: email already exists: {}", request.email());

            throw new DuplicateKeyException(
                    "Account already exists with email: " + request.email()
            );
        }

        Account account = accountMapper.toEntity(request);

        account.setAccountNumber(generateAccountNumber());
        account.setStatus(AccountStatus.ACTIVE);
        account.setDailyTransactionLimit(
                getDailyTransactionLimit(request.accountType())
        );

        Account savedAccount = accountRepository.save(account);

        log.info(
                "Account created successfully: accountNumber={}, accountType={}",
                savedAccount.getAccountNumber(),
                savedAccount.getAccountType()
        );

        return accountMapper.toResponse(savedAccount);
    }

    /**
     * Fetch complete account details using account number.
     * @param accountNumber unique account number
     * @return account details
     */
    @Override
    public AccountResponse getAccount(String accountNumber) {

        log.debug("Fetching account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Account not found: {}", accountNumber);

                    return new NotFoundException(
                            "Account not found with account number: " + accountNumber
                    );
                });

        return accountMapper.toResponse(account);
    }

    /**
     * Fetch account balance using a lightweight projection.
     * @param accountNumber unique account number
     * @return account number and current balance
     */
    @Override
    public AccountBalanceResponse getBalance(String accountNumber) {

        log.debug("Fetching balance for account: {}", accountNumber);

        AccountBalanceProjection projection =
                accountRepository.findBalanceByAccountNumber(accountNumber)
                        .orElseThrow(() -> {
                            log.warn(
                                    "Account not found while fetching balance: {}",
                                    accountNumber
                            );

                            return new NotFoundException(
                                    "Account not found with account number: " + accountNumber
                            );
                        });

        return accountMapper.toBalanceResponse(projection);
    }

    /**
     * Block an account.
     * Called by Fraud Detection Service when suspicious activity
     * is detected.
     * @param accountNumber unique account number
     * @return updated account details
     */
    @Transactional
    @Override
    public AccountResponse blockAccount(String accountNumber) {

        log.info("Blocking account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn(
                            "Account not found while blocking: {}",
                            accountNumber
                    );

                    return new NotFoundException(
                            "Account not found with account number: " + accountNumber
                    );
                });

        account.setStatus(AccountStatus.BLOCKED);

        log.info("Account blocked successfully: {}", accountNumber);

        return accountMapper.toResponse(account);
    }

    /**
     * Deduct balance from the sender account.
     * Called by Transaction Service.
     * @param accountNumber sender account number
     * @param amount        amount to deduct
     * @return updated account details
     */
    @Transactional
    @Override
    public AccountResponse deductBalance(
            String accountNumber,
            BigDecimal amount
    ) {

        log.info("Deducting balance from account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn(
                            "Account not found while deducting balance: {}",
                            accountNumber
                    );

                    return new NotFoundException(
                            "Account not found with account number: " + accountNumber
                    );
                });

        if (!AccountStatus.ACTIVE.equals(account.getStatus())) {
            log.warn(
                    "Balance deduction rejected: account is not active: {}",
                    accountNumber
            );

            throw new ForbiddenException("Account not active");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn(
                    "Balance deduction failed due to insufficient balance: {}",
                    accountNumber
            );

            throw new RuntimeException(
                    "Insufficient balance for account: " + accountNumber
            );
        }

        account.setBalance(
                account.getBalance().subtract(amount)
        );

        log.info(
                "Balance deducted successfully from account: {}, amount={}",
                accountNumber,
                amount
        );

        return accountMapper.toResponse(account);
    }

    /**
     * Credit balance to the receiver account.
     * Called by Transaction Service.
     * @param accountNumber receiver account number
     * @param amount        amount to credit
     * @return updated account details
     */
    @Transactional
    @Override
    public AccountResponse creditBalance(
            String accountNumber,
            BigDecimal amount
    ) {

        log.info("Crediting balance to account: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn(
                            "Account not found while crediting balance: {}",
                            accountNumber
                    );

                    return new NotFoundException(
                            "Account not found with account number: " + accountNumber
                    );
                });

        if (!AccountStatus.ACTIVE.equals(account.getStatus())) {
            log.warn(
                    "Balance credit rejected: account is not active: {}",
                    accountNumber
            );

            throw new ForbiddenException("Account not active");
        }

        account.setBalance(
                account.getBalance().add(amount)
        );

        log.info(
                "Balance credited successfully to account: {}, amount={}",
                accountNumber,
                amount
        );

        return accountMapper.toResponse(account);
    }


    // Helper methods
    /**
     * Get the daily transaction limit based on account type.
     * @param accountType type of bank account
     * @return daily transaction limit
     */
    private BigDecimal getDailyTransactionLimit(AccountType accountType) {

        return switch (accountType) {
            case SAVINGS -> new BigDecimal("100000");
            case CURRENT -> new BigDecimal("500000");
            case FIXED_DEPOSIT -> new BigDecimal("0");
        };
    }

    /**
     * Generate a unique 12-digit account number.
     * @return generated unique account number
     */
    private String generateAccountNumber() {

        String accountNumber;

        do {
            accountNumber = String.valueOf(
                    ThreadLocalRandom.current()
                            .nextLong(
                                    100_000_000_000L,
                                    1_000_000_000_000L
                            )
            );
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}