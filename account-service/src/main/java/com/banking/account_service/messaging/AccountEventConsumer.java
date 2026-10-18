package com.banking.account_service.messaging;

import com.banking.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountService accountService;

    /*
    * Consume transaction.completed event from Kafka
    *
    * @params payload
    * */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String , Object> payload
            ){
        try{
            String accountNumber = (String) payload.get("receiverAccountNumber");
            BigDecimal balance = (BigDecimal) payload.get("balance");

            log.info("Received transaction completed of amount: {} for account number: {}", balance ,accountNumber);

          accountService.creditBalance(accountNumber, balance);

        }catch (Exception e){
            log.error("Exception occurred while processing transaction completed event : {}" , e.getMessage());
        }
    }


    /*
     * Consume fraud.detected event from Kafka
     * Block the flagged account.
     * @params payload
     * */
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String , Object> payload
    ){
        try{
            String accountNumber = (String) payload.get("accountNumber");
            log.info("Fraud detected for account number: {}", accountNumber);

            accountService.blockAccount(accountNumber);

        } catch (Exception e) {
log.error("Exception occurred while blocking getting account number : {}" , e.getMessage());
        }

    }
}
