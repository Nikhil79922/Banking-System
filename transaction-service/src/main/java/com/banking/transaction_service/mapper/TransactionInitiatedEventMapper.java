package com.banking.transaction_service.mapper;

import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.dto.events.TransactionInitiatedEvent;
import org.springframework.stereotype.Component;

@Component
public class TransactionInitiatedEventMapper {

    public TransactionInitiatedEvent toTransactionInitiatedEvent(Transaction transaction) {
        TransactionInitiatedEvent transactionInitiatedEvent = new TransactionInitiatedEvent();
        transactionInitiatedEvent.setTransactionId(transaction.getId());
        transactionInitiatedEvent.setAmount(transaction.getAmount());
        transactionInitiatedEvent.setDescription(transaction.getDescription());
        transactionInitiatedEvent.setReceiverAccountNumber(transaction.getReceiverAccountNumber());
        transactionInitiatedEvent.setSenderAccountNumber(transaction.getSenderAccountNumber());

        return transactionInitiatedEvent;
    }
}
