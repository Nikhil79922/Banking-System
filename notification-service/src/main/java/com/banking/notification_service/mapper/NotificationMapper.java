package com.banking.notification_service.mapper;

import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.EmailTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationMapper {

    public NotificationRequest toDto(EmailTemplate template, String accountNumber,
                                     Map<String, Object> variables) {
        return NotificationRequest.builder()
                .template(template)
                .accountNumber(accountNumber)
                .variables(variables == null ? Map.of() : variables)
                .build();
    }
}