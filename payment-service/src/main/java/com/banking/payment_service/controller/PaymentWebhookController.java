
package com.banking.payment_service.controller;

import com.banking.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final PaymentService paymentService;

    /**
     * Handles Razorpay webhook events.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        log.info("Received Razorpay webhook event");

        paymentService.handleRazorpayWebhook(payload, signature);

        return ResponseEntity.ok().build();
    }

    /**
     * Handles Stripe webhook events.
     */
    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        log.info("Received Stripe webhook event");

        paymentService.handleStripeWebhook(payload, signature);

        return ResponseEntity.ok().build();
    }
}
