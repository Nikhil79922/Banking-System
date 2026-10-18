package com.banking.notification_service.service;

import com.banking.notification_service.enums.EmailTemplate;

import java.util.Map;

public interface EmailTemplateRenderer {

    String render(EmailTemplate template, Map<String, Object> variables);
}