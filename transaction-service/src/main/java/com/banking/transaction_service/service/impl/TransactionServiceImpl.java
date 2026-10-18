package com.banking.transaction_service.service.impl;

import com.banking.common.exception.NotFoundException;
import com.banking.transaction_service.client.AccountServiceClient;
import com.banking.transaction_service.dto.CreateTransactionRequest;
import com.banking.transaction_service.dto.TransactionResponse;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.entity.enums.TransactionStatus;
import com.banking.transaction_service.entity.enums.TransactionType;
import com.banking.transaction_service.mapper.FeignClientResponseMapper;
import com.banking.transaction_service.mapper.TransactionInitiatedEventMapper;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.dto.events.TransactionInitiatedEvent;
import com.banking.transaction_service.repository.Pagination;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.service.TransactionService;
import com.banking.transaction_service.service.TransactionsOps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final Pagination pagination;
    private final TransactionMapper transactionMapper;
    private final TransactionsOps transactionsOps;
    private final AccountServiceClient accountServiceClient;
    private final FeignClientResponseMapper feignClientResponseMapper;
    private final TransactionInitiatedEventMapper transactionInitiatedEventMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;



    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";


    /**
     * SAGA Step 1 :- Initiate transfer
     * <p>
     * Initiates a new transaction transfer.
     * <p>
     * The transaction will initially be created with a PENDING status
     * and processed further by the transaction workflow.
     */
    @Override
    public TransactionResponse
    transfer(CreateTransactionRequest createTransactionRequest) {
        log.info(
                "Initiating transaction transfer from account: {} to account: {}, amount: {}",
                createTransactionRequest.senderAccountNumber(),
                createTransactionRequest.receiverAccountNumber(),
                createTransactionRequest.amount()
        );

        feignClientResponseMapper.mapToDto(
                accountServiceClient.deductBalance(
                        createTransactionRequest.senderAccountNumber(),
                        createTransactionRequest.amount()
                )
        );

        Transaction transaction = transactionMapper.toEntity(createTransactionRequest);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setCreatedAt(LocalDateTime.now());

        Transaction savedTrans = transactionRepository.save(transaction);
        log.info("Saved transaction as Processing : {}", savedTrans.getId());


        //Saga Step - 2: Publish for fraud detection
        TransactionInitiatedEvent transactionInitiatedEvent = transactionInitiatedEventMapper.toTransactionInitiatedEvent(transaction);

        //Send Kafka event.
        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, transactionInitiatedEvent)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish transaction initiated event", exception);
                        return;
                    }

                    log.info(
                            "Kafka acknowledged event. Topic={}, Partition={}, Offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
        log.info("Saga Step 2 : Transaction Initiated event published: {}", transactionInitiatedEvent);

        return transactionMapper.toResponse(savedTrans);
    }

    /**
     * Fetches a transaction using its unique transaction ID.
     */
    @Override
    public TransactionResponse getTransaction(String transactionId) {

        log.info("Fetching transaction with ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found with ID: " + transactionId)
        );

        return transactionMapper.toResponse(transaction);
    }

    /**
     * Fetches all transactions associated with an account.
     * <p>
     * The account can be either the sender or receiver of the transaction.
     */

    @Override
    public List<TransactionResponse> getTransactionHistory(
            String accountNumber,
            int skip,
            int limit,
            String sort
    ) {
        log.info("Fetching transaction history for account: {}", accountNumber);

        Pageable pageable = pagination.getPageable(skip, limit, sort, "createdAt");

        Page<Transaction> transactions =
                transactionRepository.findBySenderAccountNumber(
                        accountNumber,
                        pageable
                );

        return transactions.getContent()
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    /**
     * Verifies the OTP associated with a suspicious transaction.
     * <p>
     * Successful verification should allow a PENDING_VERIFICATION
     * transaction to continue processing.
     */
    @Override
    public TransactionResponse verifyTransactionOtp(
            String transactionId,
            String otp
    ) {

        log.info("Verifying OTP for transaction ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new NotFoundException("Transaction not found with ID: " + transactionId)
        );

        String otpKey = "verification:otp:" + transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            //OTP Expired
            log.warn("OTP has been expired for transaction ID: {}", transactionId);

            transactionsOps.compensateTransaction(transaction, "OTP expired - transaction cancelled and amount refuned");
        }

        if (!otp.equals(storedOtp)) {
            //Wrong OTP -- Block account and refund.
            log.warn("OTP has been expired for transaction ID: {}", transactionId);
            redisTemplate.delete(otpKey);
            transactionsOps.blockAccountcompensate(transaction, "Wrong OTP entered - transaction cancelled , amount refunded and account blocked for security");
        }

        //Correct OTP
        log.info("Transaction with ID: {}", transactionId + " has been verified");
        redisTemplate.delete(otpKey);
        transactionsOps.completeTransaction(transaction);
        return transactionMapper.toResponse(transaction);

    }


}