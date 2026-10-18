package com.banking.payment_service.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorPayConfig {

    @Value("${razorpay.api.key}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret}")
    private String razorpaySecret;

    @Bean
    public RazorpayClient razorPayClient() {
        try {
            return new RazorpayClient(
                    razorpayApiKey,
                    razorpaySecret
            );
        } catch (Exception e) {
            throw new RuntimeException("Razorpay client error", e);
        }
    }
}
