package com.banking.payment_service.controller;


import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.dto.PaymentResponse;
import com.banking.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<SuccessResponse<PaymentLinkResponse>>
    createPayment(
@Valid @RequestBody CreatePaymentRequest createPaymentRequest
    ){
        PaymentLinkResponse  paymentLinkResponse =
                paymentService.createPaymentOrder(createPaymentRequest);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<PaymentLinkResponse>(
                        true,
                        "Payment session created successfully",
                        paymentLinkResponse,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }
}
