package com.banking.transaction_service.repository;


import com.banking.transaction_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findBySenderAccountNumber(
            String accountNumber,
            Pageable pageable
    );
}
