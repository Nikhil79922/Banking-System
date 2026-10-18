package com.banking.fraud_detection_service.service.impl;

import com.banking.fraud_detection_service.client.AccountServiceClient;
import com.banking.fraud_detection_service.dto.AccountBalanceDto;
import com.banking.fraud_detection_service.mapper.FeignClientResponseMapper;
import com.banking.fraud_detection_service.model.FraudCheckResult;
import com.banking.fraud_detection_service.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final FeignClientResponseMapper feignClientResponseMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;

    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";

    @Override
    public void checkTransaction(Map<String, Object> payload) throws Exception {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        Object amountValue = payload.get("amount");

        if (!(amountValue instanceof Number number)) {
            throw new IllegalArgumentException(
                    "Transaction amount is missing or invalid"
            );
        }

        BigDecimal amount = new BigDecimal(number.toString());

        //Fetch real balance from Account service
        AccountBalanceDto senderAccountBalanceDetail = feignClientResponseMapper.mapToDto(
                accountServiceClient.getBalance(accountNumber)
        );

        log.info("Checking transaction id {}, account number {} with remaining balance of sender : {} , for amount : {}", transactionId, accountNumber, senderAccountBalanceDetail.balance(), amount);


        //Fraud Detection check
        FraudCheckResult result = performFraudChecks(accountNumber, amount, senderAccountBalanceDetail);

        if (result.isFraud()) {
            log.info("Suspicious activity detected - account number : {}, amount : {} " + " for reason {} ", accountNumber, amount, result.getReason());

            log.info("Requesting OTP verification");

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("senderAccountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, verificationEvent);
        } else {
            //Transaction is clean -- no fraud detected

            Map<String, Object> cleanTransactionEvent = new HashMap<>();
            cleanTransactionEvent.put("transactionId", transactionId);
            cleanTransactionEvent.put("senderAccountNumber", accountNumber);
            cleanTransactionEvent.put("amount", amount);
            cleanTransactionEvent.put("reason", result.getReason());
            cleanTransactionEvent.put("isFraud", result.isFraud());

            log.info("Transaction is clean -- no fraud detected");
            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, transactionId, cleanTransactionEvent);
        }
    }

    private FraudCheckResult performFraudChecks(String accountNumber, BigDecimal amount, AccountBalanceDto senderAccountBalanceDetail) throws Exception {
        // Pattern 1) Velocity Check
        if (isVelocityCheck(accountNumber)) {
            return new FraudCheckResult(
                    true,
                    "Too many transactions in 60 seconds" +
                            " - Velocity limit exceeded"
            );
        }

        // Pattern 2) Amount Check
        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(
                    true,
                    "Unusual transactions amount" +
                            " - exceeds 3x your average"
            );
        }

        // Pattern 3) Balance check
        if (senderAccountBalanceDetail.balance().compareTo(BigDecimal.ZERO) > 0
                && isBalanceCheckFailed(senderAccountBalanceDetail.balance(), amount)) {
            return new FraudCheckResult(
                    true,
                    "Transactions exceeds 90% of account amount"
            );
        }
        return new FraudCheckResult(
                false,
                null);
    }

    private boolean isVelocityCheck(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        log.info("Velocity check account number : {}, count : {}/{}", accountNumber ,  count, maxTransactionsPerMinute);

        return count != null && count > maxTransactionsPerMinute;
    }

    private boolean isAmountSuspicious(String accountNumber , BigDecimal amount) {
        String key = "fraud:avg_amount:" + accountNumber;
        String avgStr = redisTemplate.opsForValue().get(key);

        if ( avgStr == null) {
            redisTemplate.opsForValue().set(key, amount.toString());
            return false;
        }

        BigDecimal avgAmount = new BigDecimal(avgStr);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(suspiciousAmountMultiplier));

        //update running average
        BigDecimal newAvg = avgAmount.add(amount)
                .divide(BigDecimal.valueOf(2), 2 , RoundingMode.HALF_UP);

        redisTemplate.opsForValue().set(key, newAvg.toString());
        log.info("Suspicious amount : {}, threshold : {}", newAvg, threshold);

        return newAvg.compareTo(threshold) >= 0;
    }

    private boolean isBalanceCheckFailed(BigDecimal balance, BigDecimal amount) {
        BigDecimal maxAllowed = balance.multiply(BigDecimal.valueOf(maxBalancePercentage));

        log.info("Balance check failed : {}", maxAllowed.toString());
        return maxAllowed.compareTo(amount) >= 0;
    }
}
