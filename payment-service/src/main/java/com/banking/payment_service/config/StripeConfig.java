package com.banking.payment_service.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.secret}")
    private String stripeApiSecret;

    @Bean
    public StripeClient stripeClient() {
        return new StripeClient(stripeApiSecret);
    }
}