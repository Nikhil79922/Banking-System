package com.banking.payment_service.repository;

import com.banking.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RestController
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentLinkId( String paymentLinkId );



}
