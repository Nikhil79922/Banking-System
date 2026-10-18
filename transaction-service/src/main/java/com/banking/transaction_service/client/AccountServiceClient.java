package com.banking.transaction_service.client;

import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.transaction_service.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service" , url = "${account.service.url}")
public interface AccountServiceClient {

    @PutMapping("/api/v1/accounts/{accountNumber}/deduct")
    public ResponseEntity<SuccessResponse<AccountDto>>
    deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    );

    @PutMapping("/api/v1/accounts/{accountNumber}/credit")
    public ResponseEntity<SuccessResponse<AccountDto>>
    creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    );
}
