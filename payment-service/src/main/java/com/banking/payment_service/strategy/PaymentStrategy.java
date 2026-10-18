package com.banking.payment_service.strategy;


import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.entity.Payment;
import com.banking.payment_service.entity.enums.PaymentMethod;
import com.banking.payment_service.entity.enums.PaymentOrderStatus;

public interface PaymentStrategy {

    PaymentMethod getPaymentMethod();

    PaymentLinkResponse createPayment(
            CreatePaymentRequest paymentRequest,
            Payment payment
    );


}