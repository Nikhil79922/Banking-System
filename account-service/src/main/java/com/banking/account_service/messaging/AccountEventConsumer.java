package com.banking.account_service.messaging;

import com.banking.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".account-retry",
            dltTopicSuffix = ".account-dlt"
    )
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {

        String accountNumber =
                String.valueOf(payload.get("receiverAccountNumber"));

        Object rawBalance = payload.get("balance");
        if (rawBalance == null) {
            throw new IllegalArgumentException(
                    "Missing balance in transaction.completed event"
            );
        }

        BigDecimal balance = new BigDecimal(rawBalance.toString());

        log.info(
                "Received transaction.completed: amount={}, account={}",
                balance, mask(accountNumber)
        );

        accountService.creditBalance(accountNumber, balance);

        log.info(
                "Account credited successfully: account={}",
                mask(accountNumber)
        );
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".account-retry",
            dltTopicSuffix = ".account-dlt"
    )
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload) {

        String accountNumber = String.valueOf(payload.get("accountNumber"));

        log.info(
                "Fraud detected for account={}",
                mask(accountNumber)
        );

        accountService.blockAccount(accountNumber);

        log.info(
                "Account blocked successfully: account={}",
                mask(accountNumber)
        );
    }

    @DltHandler
    public void handleDlt(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.error(
                "Account event moved to DLT: topic={}, partition={}, "
                        + "offset={}, transactionId={}, account={}",
                topic,
                partition,
                offset,
                payload.get("transactionId"),
                mask(String.valueOf(payload.get("accountNumber")))
        );

        // TODO: Persist the failed event and metadata.
        // TODO: Alert the operations team.
        // TODO: Add controlled replay after investigation.
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "****" +
                accountNumber.substring(accountNumber.length() - 4);
    }
}