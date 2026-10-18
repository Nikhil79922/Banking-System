package com.banking.payment_service.service;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;

public interface PaymentService {

    PaymentLinkResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest);

    void handleRazorpayWebhook(byte[] payload, String signature);

    void handleStripeWebhook    (byte[] payload, String signature);


}
