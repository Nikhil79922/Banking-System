package com.banking.payment_service.service.impl;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.entity.Payment;
import com.banking.payment_service.entity.enums.PaymentOrderStatus;
import com.banking.payment_service.mapper.PaymentMapper;
import com.banking.payment_service.repository.PaymentRepository;
import com.banking.payment_service.resolver.PaymentStrategyResolver;
import com.banking.payment_service.service.PaymentService;
import com.banking.payment_service.strategy.PaymentStrategy;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentStrategyResolver paymentStrategyResolver;
    private final KafkaTemplate<String, Object> kafkaTemplate ;

    @Value("${razorpay.webhook.secret:}")
    private String razorpayWebhookSecret;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    @Override
    @Transactional
    public PaymentLinkResponse createPaymentOrder(
            CreatePaymentRequest createPaymentRequest
    ) {
        log.info(
                "Initiating payment using method={}",
                createPaymentRequest.paymentMethod()
        );

        Payment payment = paymentMapper.toEntity(createPaymentRequest);
        payment.setStatus(PaymentOrderStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        try {
            PaymentStrategy strategy = paymentStrategyResolver.resolve(
                    createPaymentRequest.paymentMethod()
            );

            PaymentLinkResponse response = strategy.createPayment(
                    createPaymentRequest,
                    savedPayment
            );

            savedPayment.setPaymentLinkId(response.payment_link_id());

            paymentRepository.save(savedPayment);

            log.info(
                    "Payment link created successfully for paymentId={}",
                    savedPayment.getId()
            );

            return response;

        } catch (RuntimeException exception) {
            log.error(
                    "Payment creation failed for paymentId={}",
                    savedPayment.getId(),
                    exception
            );

            throw exception;
        }
    }

    @Override
    @Transactional
    public void handleRazorpayWebhook(byte[] payload, String signature) {
        try {
            String rawBody = new String(payload, StandardCharsets.UTF_8);

            if (!Utils.verifyWebhookSignature(
                    rawBody,
                    signature,
                    razorpayWebhookSecret
            )) {
                throw new IllegalArgumentException(
                        "Invalid Razorpay webhook signature"
                );
            }

            JSONObject event = new JSONObject(rawBody);
            String eventType = event.optString("event");

            if (!"payment_link.paid".equals(eventType)
                    && !"payment.failed".equals(eventType)) {
                log.info("Ignoring Razorpay event type={}", eventType);
                return;
            }

            JSONObject payloadObject = event.getJSONObject("payload");

            String linkId;

            if ("payment_link.paid".equals(eventType)) {
                linkId = payloadObject
                        .getJSONObject("payment_link")
                        .getJSONObject("entity")
                        .getString("id");
            } else {
                JSONObject paymentEntity = payloadObject
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                linkId = paymentEntity.optString("payment_link_id");

                if (linkId.isBlank()) {
                    log.warn(
                            "Razorpay payment.failed event has no payment_link_id"
                    );
                    return;
                }
            }

            Payment payment = paymentRepository
                    .findByPaymentLinkId(linkId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment not found for Razorpay link: " + linkId
                    ));

            if ("payment_link.paid".equals(eventType)) {
                updatePaymentStatusAndPublishEvent(
                        payment,
                        PaymentOrderStatus.SUCCESS,
                        null,
                        linkId
                );
            } else {
                updatePaymentStatusAndPublishEvent(
                        payment,
                        PaymentOrderStatus.FAILED,
                        "Payment failed via Razorpay",
                        linkId
                );
            }

        } catch (RazorpayException exception) {
            log.warn("Razorpay webhook signature verification failed");

            throw new IllegalArgumentException(
                    "Invalid Razorpay webhook signature",
                    exception
            );
        }
    }


    @Override
    @Transactional
    public void handleStripeWebhook(byte[] payload, String signature) {
        try {
            String rawBody = new String(payload, StandardCharsets.UTF_8);

            Event event = Webhook.constructEvent(
                    rawBody,
                    signature,
                    stripeWebhookSecret
            );

            String eventType = event.getType();

            boolean completed =
                    "checkout.session.completed".equals(eventType);

            boolean asyncSucceeded =
                    "checkout.session.async_payment_succeeded".equals(eventType);

            boolean asyncFailed =
                    "checkout.session.async_payment_failed".equals(eventType);

            if (!completed && !asyncSucceeded && !asyncFailed) {
                log.info("Ignoring Stripe event type={}", eventType);
                return;
            }

            StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unable to deserialize Stripe webhook event"
                    ));

            if (!(stripeObject instanceof Session session)) {
                throw new IllegalArgumentException(
                        "Unexpected Stripe webhook object"
                );
            }

            Payment payment = paymentRepository
                    .findByPaymentLinkId(session.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Payment not found for Stripe session: "
                                    + session.getId()
                    ));

            if (asyncFailed) {
                updatePaymentStatusAndPublishEvent(
                        payment,
                        PaymentOrderStatus.FAILED,
                        "Payment failed via Stripe",
                        session.getId()
                );
                return;
            }

            if ((completed || asyncSucceeded)
                    && "paid".equals(session.getPaymentStatus())) {

                updatePaymentStatusAndPublishEvent(
                        payment,
                        PaymentOrderStatus.SUCCESS,
                        null,
                        session.getId()
                );
            } else {
                log.info(
                        "Stripe payment is not yet paid for paymentId={}, status={}",
                        payment.getId(),
                        session.getPaymentStatus()
                );
            }

        } catch (SignatureVerificationException exception) {
            log.warn("Stripe webhook signature verification failed");

            throw new IllegalArgumentException(
                    "Invalid Stripe webhook signature",
                    exception
            );
        }
    }


    private void publishPaymentEvent(
            Payment payment,
            PaymentOrderStatus status,
            String reason,
            String gatewayReference
    ) {
        Map<String, Object> eventData = new HashMap<>();

        eventData.put("paymentId", payment.getId());
        eventData.put("accountNumber", payment.getAccountNumber());
        eventData.put("amount", payment.getAmount());
        eventData.put("currency", payment.getCurrency());
        eventData.put("paymentMethod", payment.getPaymentMethod());
        eventData.put("status", status);
        eventData.put("reason", reason);
        eventData.put("gatewayReference", gatewayReference);

        String topic = status == PaymentOrderStatus.SUCCESS
                ? PAYMENT_COMPLETED_TOPIC
                : PAYMENT_FAILED_TOPIC;

        kafkaTemplate.send(
                topic,
                payment.getId().toString(),
                eventData
        );

        log.info(
                "Published {} event for paymentId={}",
                topic,
                payment.getId()
        );
    }

    private void updatePaymentStatusAndPublishEvent(
            Payment payment,
            PaymentOrderStatus status,
            String reason,
            String gatewayReference
    ) {
        // Never overwrite a successful payment.
        if (payment.getStatus() == PaymentOrderStatus.SUCCESS) {
            log.info(
                    "Payment already successful; ignoring status update for paymentId={}",
                    payment.getId()
            );
            return;
        }

        payment.setStatus(status);

        if (status == PaymentOrderStatus.FAILED) {
            payment.setFailureReason(reason);
        }

        paymentRepository.save(payment);

        publishPaymentEvent(
                payment,
                status,
                reason,
                gatewayReference
        );
    }
}
