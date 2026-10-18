package com.banking.payment_service.resolver;

import com.banking.payment_service.entity.enums.PaymentMethod;
import com.banking.payment_service.strategy.PaymentStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyResolver {
    HashMap<PaymentMethod, PaymentStrategy> strategies;

    public PaymentStrategyResolver(
            List<PaymentStrategy> paymentStrategies
    ){
        this.strategies = (HashMap<PaymentMethod, PaymentStrategy>) paymentStrategies.stream()
                .collect(Collectors.toMap(
                        PaymentStrategy::getPaymentMethod,
                        Function.identity()
                ));

    }

    public PaymentStrategy resolve(PaymentMethod paymentMethod){
        PaymentStrategy strategy = strategies.get(paymentMethod);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported payment method: " + paymentMethod
            );
        }

        return strategy;
    }


}
