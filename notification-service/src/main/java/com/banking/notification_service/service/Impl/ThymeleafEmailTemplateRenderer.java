package com.banking.notification_service.service.Impl;

import com.banking.notification_service.enums.EmailTemplate;
import com.banking.notification_service.service.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ThymeleafEmailTemplateRenderer implements EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    @Value("${notification.brand.name:YourBank}")
    private String bankName;

    @Value("${notification.brand.legal-name:YourBank Ltd.}")
    private String bankLegalName;

    @Value("${notification.brand.support-email:support@yourbank.com}")
    private String supportEmail;

    @Value("${notification.brand.support-phone:1800-000-000}")
    private String supportPhone;

    @Override
    public String render(EmailTemplate template, Map<String, Object> variables) {
        Context context = new Context(Locale.ENGLISH);

        // Brand values shared by the common header/footer
        context.setVariable("bankName", bankName);
        context.setVariable("bankLegalName", bankLegalName);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("supportPhone", supportPhone);
        context.setVariable("year", Year.now().getValue());

        // Per-email values (Thymeleaf th:text HTML-escapes them automatically)
        context.setVariables(variables);

        return templateEngine.process(template.getTemplatePath(), context);
    }
}