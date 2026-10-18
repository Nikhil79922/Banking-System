package com.banking.notification_service.client;

import com.banking.common.dto.commonRes.SuccessResponse;
import com.banking.notification_service.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service" , url = "${account.service.url}")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}")
    public ResponseEntity<SuccessResponse<AccountDto>>
    getAccount(@PathVariable String accountNumber);
}
