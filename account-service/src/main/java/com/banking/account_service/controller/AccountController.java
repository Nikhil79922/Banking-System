package com.banking.account_service.controller;

import com.banking.account_service.dto.AccountBalanceResponse;
import com.banking.account_service.dto.AccountResponse;
import com.banking.account_service.dto.CreateAccountRequest;
import com.banking.account_service.service.AccountService;
import com.banking.common.dto.commonRes.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<SuccessResponse<AccountResponse>>
    createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest
    ) {
        AccountResponse resData = accountService.createAccount(createAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<AccountResponse>(
                        true,
                        "Account created successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.CREATED.value()
                ));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<SuccessResponse<AccountResponse>>
    getAccount(@PathVariable String accountNumber) {
        AccountResponse resData = accountService.getAccount(accountNumber);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<AccountResponse>(
                        true,
                        "Account details fetched successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<SuccessResponse<AccountBalanceResponse>>
    getBalance(@PathVariable String accountNumber) {
        AccountBalanceResponse resData = accountService.getBalance(accountNumber);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<AccountBalanceResponse>(
                        true,
                        "Account balance successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }


    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<SuccessResponse<AccountResponse>>
    blockAccount(@PathVariable String accountNumber) {
        AccountResponse resData = accountService.blockAccount(accountNumber);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<AccountResponse>(
                        true,
                        "Account blocked successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }

    /**
     * SAGA STEP 1 - Deduct Balance
     * Called by Transaction Service when transfer is initiated
     */

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<SuccessResponse<AccountResponse>>
    deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        AccountResponse resData = accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<AccountResponse>(
                        true,
                        "Account balance deducted successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }

    /**
     * SAGA STEP 4 - Compensating transaction endpoint
     * Called by Transaction Service in two scenarios:
     *   1. Fraud detected -> refund sender ( undo step 1 )
     *   2. Transaction completed -> credit receiver
     */


    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<SuccessResponse<AccountResponse>>
    creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ) {
        AccountResponse resData = accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new SuccessResponse<AccountResponse>(
                        true,
                        "Account balance credited successfully",
                        resData,
                        LocalDateTime.now(),
                        HttpStatus.OK.value()
                ));
    }


}
