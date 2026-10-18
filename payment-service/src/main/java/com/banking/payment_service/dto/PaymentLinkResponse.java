package com.banking.payment_service.dto;


public record PaymentLinkResponse(
         String payment_link_url,
         String payment_link_id
) {
}
