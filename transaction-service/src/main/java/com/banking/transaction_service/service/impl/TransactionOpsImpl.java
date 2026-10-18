package com.banking.transaction_service.service.impl;

import com.banking.common.exception.NotFoundException;
import com.banking.transaction_service.client.AccountServiceClient;
import com.banking.transaction_service.dto.events.TransactionCompletedEvent;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.service.TransactionsOps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionOpsImpl implements TransactionsOps {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionMapper transactionMapper;

    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String TRANSACTION_FRAUD_DETECTED_TOPIC = "fraud.detected";

    public void compensateTransaction(Transaction transaction, String reason) {
        log.warn("SAGA compensation -- refunding: {} , amount : {} ", transaction.getId(), transaction.getAmount());

        //Credit Money back to sender synchronously
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount());
        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason + "Saga compensation executed, amount refunded at" + LocalDateTime.now());
        transactionRepository.save(transaction);

        //Publish event to Notification service for refund acknowledgement to sender user
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", reason);

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent);

        log.info("Saga compensation completed for amount {} and refunded to : {}", transaction.getAmount(), transaction.getSenderAccountNumber());

    }

    public void blockAccountcompensate(Transaction transaction, String reason) {

        // publish fraud.detected -> Account service will block account
        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId", transaction.getId());
        fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
        fraudEvent.put("reason", reason);

        //Public event
        kafkaTemplate.send(TRANSACTION_FRAUD_DETECTED_TOPIC, transaction.getSenderAccountNumber(), fraudEvent);

        log.warn("fraud.detected event published -- account: {} , will be blocked", transaction.getSenderAccountNumber());

        //SAGA Compensation - refund Sender.
        compensateTransaction(transaction, reason);
    }

    public void completeTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        TransactionCompletedEvent transactionCompletedEvent = transactionMapper.toCompletedEvent(transaction);

        //Public Transaction completed event.
        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), transactionCompletedEvent);

        log.info("SAGA Completed -- Transaction with ID: {}", transaction.getId() + " has been completed");

    }

    @Override
    public void processCleanResult(String transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found with ID: " + transactionId)
        );

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            log.warn("Transaction {} is not PROCESSING -- skipping it ", transactionId);
            return;
        }

        completeTransaction(transaction);
    }
}
