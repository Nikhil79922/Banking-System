package com.banking.fraud_detection_service.model;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class FraudCheckResult {
    private boolean isFraud;
    private String reason;
}
