package com.banking.transaction_service.service;

import com.banking.transaction_service.entity.Transaction;

public interface TransactionsOps {

    void compensateTransaction(Transaction transaction, String reason);

    void blockAccountcompensate(Transaction transaction, String reason);

    void completeTransaction(Transaction transaction);

    void processCleanResult( String transactionId);
}
