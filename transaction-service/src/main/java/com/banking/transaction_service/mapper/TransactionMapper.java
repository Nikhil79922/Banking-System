package com.banking.transaction_service.mapper;

import com.banking.transaction_service.dto.CreateTransactionRequest;
import com.banking.transaction_service.dto.TransactionResponse;
import com.banking.transaction_service.dto.events.TransactionCompletedEvent;
import com.banking.transaction_service.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(CreateTransactionRequest request) {

        Transaction transaction = new Transaction();

        transaction.setSenderAccountNumber(
                request.senderAccountNumber()
        );

        transaction.setReceiverAccountNumber(
                request.receiverAccountNumber()
        );

        transaction.setAmount(
                request.amount()
        );

        transaction.setDescription(
                request.description()
        );

        return transaction;
    }

    public TransactionResponse toResponse(Transaction transaction) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getReferenceNumber(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }

    public TransactionCompletedEvent  toCompletedEvent(Transaction transaction) {
        return new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription()
        );
    }
}