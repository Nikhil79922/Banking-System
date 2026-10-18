package com.banking.fraud_detection_service.client;

import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.fraud_detection_service.dto.AccountBalanceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service" , url = "${account.service.url}")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}/balance")
    public ResponseEntity<SuccessResponse<AccountBalanceDto>>
    getBalance(@PathVariable String accountNumber);
}
