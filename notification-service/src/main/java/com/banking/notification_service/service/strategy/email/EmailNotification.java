package com.banking.notification_service.service.strategy.email;

import com.banking.notification_service.client.AccountServiceClient;
import com.banking.notification_service.dto.AccountDto;
import com.banking.notification_service.dto.NotificationRequest;
import com.banking.notification_service.enums.EmailTemplate;
import com.banking.notification_service.enums.NotificationTypes;
import com.banking.notification_service.mapper.FeignClientResponseMapper;
import com.banking.notification_service.service.EmailSender;
import com.banking.notification_service.service.EmailTemplateRenderer;
import com.banking.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotification
        implements NotificationService {

    private final AccountServiceClient accountServiceClient;
    private final FeignClientResponseMapper responseMapper;
    private final EmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;



    @Override
    public NotificationTypes getNotificationType() {
        return NotificationTypes.EMAIL;
    }
     // NEW

    @Override
    public boolean sendNotification(NotificationRequest request) {
        String maskedAccount = maskAccountNumber(request.getAccountNumber());
        EmailTemplate template = request.getTemplate();

        log.info("Starting {} email for account: {}", template, maskedAccount);

        AccountDto account;
        try {
            account = responseMapper.mapToDto(
                    accountServiceClient.getAccount(request.getAccountNumber()));
        } catch (Exception exception) {
            log.error("Failed to fetch account details for account: {}", maskedAccount, exception);
            return false;
        }

        if (account.email() == null || account.email().isBlank()) {
            log.warn("Recipient email is missing for account: {}", maskedAccount);
            return false;
        }

        String html;
        try {
            Map<String, Object> variables = new HashMap<>(request.getVariables());
            variables.put("accountNumber", maskedAccount);          // never email the raw number
            html = templateRenderer.render(template, variables);
        } catch (Exception exception) {
            log.error("Failed to render {} template", template, exception);
            return false;
        }

        try {
            emailSender.send(account.email(), template.getSubject(), html);
            log.info("{} email sent for account: {}", template, maskedAccount);
            return true;
        } catch (MailException exception) {
            log.error("Failed to send {} email for account: {}", template, maskedAccount, exception);
            return false;
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}