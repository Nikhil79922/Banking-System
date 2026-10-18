package com.banking.notification_service.dto;

import com.banking.notification_service.enums.EmailTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private String accountNumber;
    private EmailTemplate template;
    private Map<String, Object> variables;
}