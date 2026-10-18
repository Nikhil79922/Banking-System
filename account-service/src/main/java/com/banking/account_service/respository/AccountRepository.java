package com.banking.account_service.respository;

import com.banking.account_service.entity.Account;
import com.banking.account_service.projection.AccountBalanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Boolean existsByEmail(String email);

    Boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<AccountBalanceProjection> findBalanceByAccountNumber(
            String accountNumber
    );
}
