package com.banking.notification_service.enums;

import lombok.Getter;

/**
 * One entry per email type. Each maps to a Thymeleaf template under
 * src/main/resources/templates/email/ and carries the subject line,
 * so the subject is no longer a string scattered through the consumer.
 */
@Getter
public enum EmailTemplate {

    OTP_VERIFICATION("email/otp-verification", "Transaction Verification Required"),
    DEBIT_ALERT("email/debit-alert", "Debit Alert"),
    CREDIT_ALERT("email/credit-alert", "Credit Alert"),
    FRAUD_ALERT("email/fraud-alert", "Suspicious Activity Detected"),
    REFUND_PROCESSED("email/refund-processed", "Refund Processed"),
    PAYMENT_SUCCESS("email/payment-success", "Payment Successful"),
    PAYMENT_FAILED("email/payment-failed", "Payment Failed");

    private final String templatePath;
    private final String subject;

    EmailTemplate(String templatePath, String subject) {
        this.templatePath = templatePath;
        this.subject = subject;
    }
}