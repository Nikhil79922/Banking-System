package com.banking.notification_service.messaging;

import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.EmailTemplate;
import com.banking.notification_service.enums.NotificationTypes;
import com.banking.notification_service.mapper.NotificationMapper;
import com.banking.notification_service.service.strategyResolvers.NotificationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final long OTP_EXPIRY_MINUTES = 5;

    private final NotificationResolver notificationResolver;
    private final NotificationMapper notificationMapper;

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> p) {

        String account = str(p, "accountNumber");

        log.info("Received transaction.otp.generated: account={}",
                mask(account));

        requireSuccess(dispatch(
                "transaction.otp.generated",
                mask(account),
                EmailTemplate.OTP_VERIFICATION,
                account,
                Map.of(
                        "amount", str(p, "amount"),
                        "reason", str(p, "reason"),
                        "otp", str(p, "otp"),
                        "expiryMinutes", OTP_EXPIRY_MINUTES
                )
        ));
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> p) {

        String transactionId = str(p, "transactionId");

        log.info("Received transaction.completed: transactionId={}",
                transactionId);

        Map<String, Object> vars = Map.of(
                "amount", str(p, "amount"),
                "transactionId", transactionId
        );

        // Attempt both notifications, even if the first one fails.
        boolean debitSent = dispatch(
                "transaction.completed[debit]",
                transactionId,
                EmailTemplate.DEBIT_ALERT,
                str(p, "senderAccountId"),
                vars
        );

        boolean creditSent = dispatch(
                "transaction.completed[credit]",
                transactionId,
                EmailTemplate.CREDIT_ALERT,
                str(p, "receiverAccountId"),
                vars
        );

        // Propagate failure so Spring Kafka can retry the event.
        if (!debitSent || !creditSent) {
            throw new IllegalStateException(
                    "One or more transaction completion notifications failed: "
                            + transactionId
            );
        }
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> p) {

        String transactionId = str(p, "transactionId");

        log.info("Received fraud.detected: transactionId={}",
                transactionId);

        requireSuccess(dispatch(
                "fraud.detected",
                transactionId,
                EmailTemplate.FRAUD_ALERT,
                str(p, "accountNumber"),
                Map.of(
                        "transactionId", transactionId,
                        "reason", str(p, "reason")
                )
        ));
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String, Object> p) {

        String transactionId = str(p, "transactionId");

        log.info("Received transaction.refunded: transactionId={}",
                transactionId);

        requireSuccess(dispatch(
                "transaction.refunded",
                transactionId,
                EmailTemplate.REFUND_PROCESSED,
                str(p, "senderAccountNumber"),
                Map.of(
                        "amount", str(p, "amount"),
                        "transactionId", transactionId,
                        "reason", str(p, "reason")
                )
        ));
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String, Object> p) {

        String paymentId = str(p, "paymentId");

        log.info("Received payment.completed: paymentId={}", paymentId);

        requireSuccess(dispatch(
                "payment.completed",
                paymentId,
                EmailTemplate.PAYMENT_SUCCESS,
                str(p, "accountNumber"),
                Map.of(
                        "amount", str(p, "amount"),
                        "currency", str(p, "currency"),
                        "paymentId", paymentId,
                        "paymentMethod", str(p, "paymentMethod"),
                        "gatewayReference", str(p, "gatewayReference")
                )
        ));
    }

    @RetryableTopic(
            attempts = "4",
            retryTopicSuffix = ".notification-retry",
            dltTopicSuffix = ".notification-dlt"
    )
    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String, Object> p) {

        String paymentId = str(p, "paymentId");

        log.info("Received payment.failed: paymentId={}", paymentId);

        requireSuccess(dispatch(
                "payment.failed",
                paymentId,
                EmailTemplate.PAYMENT_FAILED,
                str(p, "accountNumber"),
                Map.of(
                        "amount", str(p, "amount"),
                        "currency", str(p, "currency"),
                        "paymentId", paymentId,
                        "paymentMethod", str(p, "paymentMethod"),
                        "reason", str(p, "reason")
                )
        ));
    }

    @DltHandler
    public void handleDlt(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.error(
                "Notification event moved to DLT: topic={}, "
                        + "transactionId={}, paymentId={}, account={}",
                topic,
                payload.get("transactionId"),
                payload.get("paymentId"),
                mask(str(payload, "accountNumber"))
        );

        // TODO:
        // Persist the failed event and metadata in PostgreSQL.
        // Alert the operations team.
        // Support controlled replay after the cause is resolved.
    }

    private boolean dispatch(
            String event,
            String reference,
            EmailTemplate template,
            String accountNumber,
            Map<String, Object> variables) {

        try {
            NotificationRequest request =
                    notificationMapper.toDto(
                            template,
                            accountNumber,
                            variables
                    );

            boolean sent = notificationResolver
                    .getNotificationStrategy(NotificationTypes.EMAIL)
                    .sendNotification(request);

            if (!sent) {
                log.warn(
                        "{} notification failed: ref={}",
                        event,
                        reference
                );
                return false;
            }

            log.info(
                    "{} notification sent: ref={}",
                    event,
                    reference
            );

            return true;

        } catch (Exception e) {
            log.error(
                    "Error processing notification: event={}, ref={}",
                    event,
                    reference,
                    e
            );

            return false;
        }
    }

    private void requireSuccess(boolean sent) {
        if (!sent) {
            throw new IllegalStateException(
                    "Notification delivery failed"
            );
        }
    }

    private static String str(
            Map<String, Object> payload,
            String key) {

        Object value = payload.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "****" +
                accountNumber.substring(accountNumber.length() - 4);
    }
}