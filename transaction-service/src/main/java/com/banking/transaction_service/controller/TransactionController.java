package com.banking.transaction_service.controller;


import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.transaction_service.dto.CreateTransactionRequest;
import com.banking.transaction_service.dto.TransactionResponse;
import com.banking.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<SuccessResponse<TransactionResponse>>
    transfer(
            @Valid @RequestBody CreateTransactionRequest createTransactionRequest
            ) {
        TransactionResponse resData = transactionService.transfer(createTransactionRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<TransactionResponse>(
                        true,
                        "Transaction created successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.CREATED.value()
                ));
    }


    @GetMapping("/{transactionId}")
    public ResponseEntity<SuccessResponse<TransactionResponse>>
    getTransaction(
            @PathVariable String transactionId
    ) {
        TransactionResponse resData = transactionService.getTransaction(transactionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<TransactionResponse>(
                        true,
                        "Transaction fetched successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<SuccessResponse<List<TransactionResponse>>>
    getTransactionHistory(
            @PathVariable String accountNumber,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Skip cannot be negative")
            int skip,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 1000, message = "Limit cannot exceed 100")
            int limit,

            @RequestParam(required = false)
            @Pattern(
                    regexp = "^(asc|desc)$",
                    message = "Sort must be either 'asc' or 'desc'"
            )
            String sort
    ) {
        List<TransactionResponse> resData =
                transactionService.getTransactionHistory(
                        accountNumber,
                        skip,
                        limit,
                        sort
                );        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<List<TransactionResponse>>(
                        true,
                        "Transaction history fetched successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }

    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<SuccessResponse<TransactionResponse>>
    verifyTransactionOtp(
            @PathVariable String transactionId,
            @RequestParam String otp
    ) {
        TransactionResponse resData = transactionService.verifyTransactionOtp(transactionId,otp);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<TransactionResponse>(
                        true,
                        "Transaction otp verified successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }
}
