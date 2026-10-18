package com.banking.payment_service.mapper;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.dto.PaymentResponse;
import com.banking.payment_service.dto.UpdatePaymentRequest;
import com.banking.payment_service.entity.Payment;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(CreatePaymentRequest request) {
        Payment payment = new Payment();

        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setAccountNumber(request.accountNumber());
        payment.setCurrency(request.currency());
        payment.setDescription(request.description());

        return payment;
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaymentLinkId(),
                payment.getAccountNumber(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public void updateEntity(
            UpdatePaymentRequest request,
            Payment payment
    ) {
        if (request.description() != null) {
            payment.setDescription(request.description());
        }
    }



    public PaymentLinkResponse toLinkRazorPayResponse(com.razorpay.PaymentLink payment) {
        return new PaymentLinkResponse(
                payment.get("short_url"),
                payment.get("id")
        );
    }

    public PaymentLinkResponse toStripePaymentLinkResponse(Session session) {
        return new PaymentLinkResponse(
                session.getUrl(),
                session.getId()
        );
    }
}