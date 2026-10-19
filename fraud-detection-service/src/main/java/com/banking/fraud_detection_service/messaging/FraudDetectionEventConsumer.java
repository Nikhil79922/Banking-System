package com.banking.fraud_detection_service.messaging;

import com.banking.fraud_detection_service.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".fraud-retry",
            dltTopicSuffix = ".fraud-dlt"
    )
    @KafkaListener(
            topics = "transaction.initiated",
            groupId = "fraud-detection-group"
    )
    public void consumeTransactionInitiated(
            @Payload Map<String, Object> payload
    ) throws Exception {
        log.info(
                "Received transaction initiated event: transactionId={}",
                payload.get("transactionId")
        );

        fraudDetectionService.checkTransaction(payload);

        log.info(
                "Fraud check completed for transactionId={}",
                payload.get("transactionId")
        );
    }

    @DltHandler
    public void handleDlt(
            @Payload Map<String, Object> payload) {

        log.error(
                "Transaction event moved to DLT: transactionId={}",
                payload.get("transactionId")
        );

        // TODO:
        // Persist the failed event in database for investigation.
        // Alert the operations team.
    }
}