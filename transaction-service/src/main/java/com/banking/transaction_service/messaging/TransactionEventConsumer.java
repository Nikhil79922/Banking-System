package com.banking.transaction_service.messaging;

import com.banking.common.exception.NotFoundException;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.service.TransactionsOps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionsOps transactionsOps;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC =
            "transaction.otp.generated";

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".transaction-retry",
            dltTopicSuffix = ".transaction-dlt"
    )
    @KafkaListener(topics = "verification.required")
    public void consumeVerificationRequired(
            @Payload Map<String, Object> payload) throws Exception {

        String accountNumber =
                requiredString(payload, "senderAccountNumber");
        String transactionId =
                requiredString(payload, "transactionId");
        String reason = requiredString(payload, "reason");

        log.info(
                "Verification required: transactionId={}, reason={}",
                transactionId, reason
        );

        Transaction transaction = transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new NotFoundException(
                        "Transaction with id " + transactionId + " not found"
                ));

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            log.warn(
                    "Transaction {} is not PROCESSING; skipping",
                    transactionId
            );
            return;
        }

        String otp = String.format(
                "%06d",
                (int) (Math.random() * 900000) + 100000
        );

        String otpKey = "verification:otp:" + transactionId;

        redisTemplate.opsForValue().set(
                otpKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES
        );

        transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
        transactionRepository.save(transaction);

        Map<String, String> otpEventPayload = new HashMap<>();
        otpEventPayload.put("accountNumber", accountNumber);
        otpEventPayload.put("transactionId", transactionId);
        otpEventPayload.put("reason", reason);
        otpEventPayload.put("otp", otp);
        otpEventPayload.put("amount", requiredString(payload, "amount"));

        // Wait for Kafka's send result so send failures reach retry handling.
        kafkaTemplate.send(
                TRANSACTION_OTP_GENERATED_TOPIC,
                transactionId,
                otpEventPayload
        ).get();

        log.info(
                "OTP event published for transaction {}; expiry={} minutes",
                transactionId, OTP_EXPIRY_MINUTES
        );
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".transaction-retry",
            dltTopicSuffix = ".transaction-dlt"
    )
    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanEvent(
            @Payload Map<String, Object> payload) {

        String transactionId = requiredString(payload, "transactionId");

        log.info(
                "Received fraud.check.clean: transactionId={}",
                transactionId
        );

        transactionsOps.processCleanResult(transactionId);

        log.info(
                "Clean fraud result processed: transactionId={}",
                transactionId
        );
    }

    @DltHandler
    public void handleDlt(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {

        log.error(
                "Transaction event moved to DLT: topic={}, partition={}, "
                        + "offset={}, transactionId={}",
                topic,
                partition,
                offset,
                payload.get("transactionId")
        );

        // TODO:
        // 1. Persist the failed event and metadata in the database.
        // 2. Alert the operations team.
        // 3. Support controlled replay after resolving the root cause.
    }

    private static String requiredString(
            Map<String, Object> payload,
            String key) {

        Object value = payload.get(key);

        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required event field: " + key
            );
        }

        return String.valueOf(value);
    }
}