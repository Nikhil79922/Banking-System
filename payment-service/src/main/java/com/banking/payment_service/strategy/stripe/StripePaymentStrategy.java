
package com.banking.payment_service.strategy.stripe;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.entity.Payment;
import com.banking.payment_service.entity.enums.PaymentMethod;
import com.banking.payment_service.mapper.PaymentMapper;
import com.banking.payment_service.strategy.PaymentStrategy;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentStrategy implements PaymentStrategy {

    private final PaymentMapper paymentMapper;
    private final StripeClient stripeClient;

    /**
     * Identifies this strategy as the Stripe payment gateway.
     *
     * @return STRIPE payment method
     */
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.STRIPE;
    }

    /**
     * Creates a Stripe Checkout Session for a payment.
     *
     * FLOW:
     * 1. Read the amount from the validated request.
     * 2. Create a Stripe Checkout Session.
     * 3. Attach the internal payment ID as metadata.
     * 4. Store the Stripe session ID on the payment entity.
     * 5. Map the Stripe response to PaymentLinkResponse.
     * 6. Return the checkout details to the caller.
     *
     * @param paymentRequest validated payment request
     * @param payment persisted payment entity
     * @return Stripe checkout details
     */
    @Override
    public PaymentLinkResponse createPayment(
            CreatePaymentRequest paymentRequest,
            Payment payment
    ) {
        log.info(
                "Creating Stripe Checkout Session for paymentId={}",
                payment.getId()
        );

        // Get the payment amount from the validated request.
        BigDecimal amount = paymentRequest.amount();

        // Create the Stripe Checkout Session.
        Session stripeSession = createStripeLink(
                amount,
                payment.getId(),
                payment.getCurrency(),
                payment.getDescription()
        );

        // Store the Stripe session ID for reconciliation and webhooks.
        payment.setPaymentLinkId(stripeSession.getId());

        // Convert the Stripe response into our application DTO.
        PaymentLinkResponse response =
                paymentMapper.toStripePaymentLinkResponse(stripeSession);

        log.info(
                "Stripe Checkout Session created successfully for paymentId={}",
                payment.getId()
        );

        return response;
    }

    /**
     * Creates a Stripe Checkout Session.
     *
     * Stripe expects the amount in the smallest currency unit.
     * Example: INR 1500.00 becomes 150000 paise.
     *
     * @param amount payment amount
     * @param paymentId internal payment identifier
     * @param currency ISO currency code
     * @param description payment description
     * @return Stripe Checkout Session
     */
    private Session createStripeLink(
            BigDecimal amount,
            Object paymentId,
            String currency,
            String description
    ) {
        try {
            // Convert the amount to the smallest currency unit.
            // Rounding is deliberately disallowed for invalid precision.
            long stripeAmount = amount
                    .movePointRight(2)
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();

            // Configure the Stripe Checkout Session.
            SessionCreateParams params =
                    SessionCreateParams.builder()

                            // Enable card payments.
                            .addPaymentMethodType(
                                    SessionCreateParams.PaymentMethodType.CARD
                            )

                            // Configure a one-time payment.
                            .setMode(
                                    SessionCreateParams.Mode.PAYMENT
                            )

                            // Redirect the customer after checkout.
                            .setSuccessUrl(
                                    "http://localhost:3000/payment-success/"
                                            + paymentId
                                            + "?session_id={CHECKOUT_SESSION_ID}"
                            )

                            // Redirect the customer when checkout is cancelled.
                            .setCancelUrl(
                                    "http://localhost:3000/payment/cancel"
                            )

                            // Attach our internal payment ID to the session.
                            .putMetadata(
                                    "paymentId",
                                    String.valueOf(paymentId)
                            )

                            // Attach the payment ID to the PaymentIntent too.
                            .setPaymentIntentData(
                                    SessionCreateParams.PaymentIntentData
                                            .builder()
                                            .putMetadata(
                                                    "paymentId",
                                                    String.valueOf(paymentId)
                                            )
                                            .build()
                            )

                            // Add the payment amount as a single line item.
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem
                                                            .PriceData.builder()
                                                            .setCurrency(
                                                                    currency.toLowerCase()
                                                            )
                                                            .setUnitAmount(
                                                                    stripeAmount
                                                            )
                                                            .setProductData(
                                                                    SessionCreateParams
                                                                            .LineItem
                                                                            .PriceData
                                                                            .ProductData
                                                                            .builder()
                                                                            .setName(
                                                                                    description != null
                                                                                            ? description
                                                                                            : "Payment #"
                                                                                              + paymentId
                                                                            )
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();

            // Send the request to Stripe and return its session.
            return stripeClient
                    .checkout()
                    .sessions()
                    .create(params);

        } catch (StripeException exception) {
            log.error(
                    "Stripe API failed while creating Checkout Session "
                            + "for paymentId={}",
                    paymentId,
                    exception
            );

            throw new IllegalStateException(
                    "Failed to create Stripe Checkout Session",
                    exception
            );

        } catch (ArithmeticException exception) {
            log.error(
                    "Invalid amount or currency precision for paymentId={}",
                    paymentId,
                    exception
            );

            throw new IllegalArgumentException(
                    "Payment amount has invalid precision or is out of range",
                    exception
            );
        }
    }
}
