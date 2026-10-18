package com.banking.transaction_service.messaging;

import com.banking.common.exception.NotFoundException;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.service.TransactionService;
import com.banking.transaction_service.service.TransactionsOps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final TransactionsOps transactionsOps;
    private final RedisTemplate<String, String> redisTemplate;
    private static final long OTP_EXPIRY_MINUTES = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";


    /*
     * Consume verification.required event from Kafka
     * Generate OTP and ask user to verify
     *
     * @params payload
     * */
    @KafkaListener(topics = "verification.required")
    public void consumeVerificationRequired(
            @Payload Map<String, Object> payload
    ) {
        try {
            String accountNumber = (String) payload.get("senderAccountNumber");
            String transactionId = (String) payload.get("transactionId");
            String reason = (String) payload.get("reason");

            log.info("Verification required for transaction id {} and account number {} , dues to this reason : {}", transactionId, accountNumber, reason);

            Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                    () -> new NotFoundException("Transaction with id " + transactionId + " not found")
            );

            if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction {} is not PROCESSING -- skipping it ", transactionId);
                return;
            }

            //generate 6 digit otp
            String otp  = String.format("%06d", (int) (Math.random() * 900000) + 100000);

            // Store Otp in redis - and expires in 5mins.
            String otpKey = "verification:otp:"+transactionId;
            redisTemplate.opsForValue().set(otpKey ,otp ,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

            //Update Transaction Status
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);

            log.info("OTP generated for transaction {} and expires in {} minutes", transactionId, OTP_EXPIRY_MINUTES);

            //Notify senders account users
            Map<String, String> otpEventPayload = new HashMap<>();
            otpEventPayload.put("accountNumber", accountNumber);
            otpEventPayload.put("transactionId", transactionId);
            otpEventPayload.put("reason", reason);
            otpEventPayload.put("otp", otp);
            otpEventPayload.put("amount", payload.get("amount").toString());

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC , transactionId , otpEventPayload);

        } catch (Exception e) {
            log.error("Exception occurred while processing verification required event : {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanEvent(
            @Payload Map<String, Object> payload
    ){
        try {
            String transactionId = (String) payload.get("transactionId");
            transactionsOps.processCleanResult(transactionId);

        }catch (Exception e){
            log.error("Error occur while consuming the fraud check clean Event -- error : {}",e.getMessage());
        }
    }

}
