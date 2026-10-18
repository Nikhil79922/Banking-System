package com.banking.fraud_detection_service.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface FraudDetectionService {
    public void checkTransaction(Map<String, Object> payload) throws Exception;
}
