package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.CreateTransactionRequest;
import com.banking.transaction_service.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse transfer(
            CreateTransactionRequest createTransactionRequest
    );

    TransactionResponse getTransaction(
            String transactionId
    );

    List<TransactionResponse> getTransactionHistory(
            String accountNumber,
            int skip ,
            int limit,
            String sort
    );

    TransactionResponse verifyTransactionOtp(
            String transactionId,
            String otp
    );
}