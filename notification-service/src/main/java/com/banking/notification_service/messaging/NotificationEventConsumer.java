package com.banking.notification_service.messaging;

import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.EmailTemplate;
import com.banking.notification_service.enums.NotificationTypes;
import com.banking.notification_service.mapper.NotificationMapper;
import com.banking.notification_service.service.strategyResolvers.NotificationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(@Payload Map<String, Object> p) {
        String account = str(p, "accountNumber");
        log.info("Received transaction.otp.generated: account={}", mask(account));

        dispatch("transaction.otp.generated", mask(account),
                EmailTemplate.OTP_VERIFICATION, account, Map.of(
                        "amount", str(p, "amount"),
                        "reason", str(p, "reason"),
                        "otp", str(p, "otp"),
                        "expiryMinutes", OTP_EXPIRY_MINUTES));
    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> p) {
        String transactionId = str(p, "transactionId");
        log.info("Received transaction.completed: transactionId={}", transactionId);

        Map<String, Object> vars = Map.of(
                "amount", str(p, "amount"),
                "transactionId", transactionId);

        // Independent dispatches: a failed debit mail must not block the credit mail
        dispatch("transaction.completed[debit]", transactionId,
                EmailTemplate.DEBIT_ALERT, str(p, "senderAccountId"), vars);
        dispatch("transaction.completed[credit]", transactionId,
                EmailTemplate.CREDIT_ALERT, str(p, "receiverAccountId"), vars);
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> p) {
        String transactionId = str(p, "transactionId");
        log.info("Received fraud.detected: transactionId={}", transactionId);

        dispatch("fraud.detected", transactionId,
                EmailTemplate.FRAUD_ALERT, str(p, "accountNumber"), Map.of(
                        "transactionId", transactionId,
                        "reason", str(p, "reason")));
    }

    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(@Payload Map<String, Object> p) {
        String transactionId = str(p, "transactionId");
        log.info("Received transaction.refunded: transactionId={}", transactionId);

        dispatch("transaction.refunded", transactionId,
                EmailTemplate.REFUND_PROCESSED, str(p, "senderAccountNumber"), Map.of(
                        "amount", str(p, "amount"),
                        "transactionId", transactionId,
                        "reason", str(p, "reason")));
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(@Payload Map<String, Object> p) {
        String paymentId = str(p, "paymentId");
        log.info("Received payment.completed: paymentId={}", paymentId);

        dispatch("payment.completed", paymentId,
                EmailTemplate.PAYMENT_SUCCESS, str(p, "accountNumber"), Map.of(
                        "amount", str(p, "amount"),
                        "currency", str(p, "currency"),
                        "paymentId", paymentId,
                        "paymentMethod", str(p, "paymentMethod"),
                        "gatewayReference", str(p, "gatewayReference")));
    }

    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(@Payload Map<String, Object> p) {
        String paymentId = str(p, "paymentId");
        log.info("Received payment.failed: paymentId={}", paymentId);

        dispatch("payment.failed", paymentId,
                EmailTemplate.PAYMENT_FAILED, str(p, "accountNumber"), Map.of(
                        "amount", str(p, "amount"),
                        "currency", str(p, "currency"),
                        "paymentId", paymentId,
                        "paymentMethod", str(p, "paymentMethod"),
                        "reason", str(p, "reason")));
    }

    private void dispatch(String event, String reference, EmailTemplate template,
                          String accountNumber, Map<String, Object> variables) {
        try {
            NotificationRequest request =
                    notificationMapper.toDto(template, accountNumber, variables);

            boolean sent = notificationResolver
                    .getNotificationStrategy(NotificationTypes.EMAIL)
                    .sendNotification(request);

            if (sent) {
                log.info("{} notification sent: ref={}", event, reference);
            } else {
                log.warn("{} notification not delivered: ref={}", event, reference);
            }
        } catch (Exception e) {
            log.error("Error processing {}: ref={}", event, reference, e);
        }
    }

    private static String str(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "-" : String.valueOf(value);
    }

    private static String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}