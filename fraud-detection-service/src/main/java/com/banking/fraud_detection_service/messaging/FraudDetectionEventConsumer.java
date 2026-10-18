package com.banking.fraud_detection_service.messaging;

import com.banking.fraud_detection_service.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService  fraudDetectionService;

    @KafkaListener(
            topics = "transaction.initiated",
            groupId = "fraud-dectection-group"
    )
    public void consumeTransactionInitiated(
            @Payload Map<String, Object> payload
    ) {
        log.info("Received transaction initiated event: {}", payload);

        try {
            fraudDetectionService.checkTransaction(payload);

            log.info(
                    "Fraud check completed for transactionId={}",
                    payload.get("transactionId")
            );
        } catch (Exception e) {
            log.error("Failed to process transaction initiated event", e);
        }
    }
}
