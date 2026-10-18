
package com.banking.payment_service.strategy.razorpay;

import com.banking.payment_service.dto.CreatePaymentRequest;
import com.banking.payment_service.dto.PaymentLinkResponse;
import com.banking.payment_service.entity.Payment;
import com.banking.payment_service.entity.enums.PaymentMethod;
import com.banking.payment_service.mapper.PaymentMapper;
import com.banking.payment_service.strategy.PaymentStrategy;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentStrategy implements PaymentStrategy {

    private final PaymentMapper paymentMapper;
    private final RazorpayClient razorpayClient;

    /**
     * Identifies this strategy as the Razorpay payment provider.
     *
     * @return RAZORPAY payment method
     */
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.RAZORPAY;
    }

    /**
     * Creates a Razorpay payment link for an existing payment record.
     *
     * FLOW:
     * 1. Read the requested payment amount.
     * 2. Create a payment link through Razorpay.
     * 3. Map the provider response into our application DTO.
     *
     * @param paymentRequest incoming payment request
     * @param payment persisted payment entity
     * @return payment link details for the frontend
     */
    @Override
    public PaymentLinkResponse createPayment(
            CreatePaymentRequest paymentRequest,
            Payment payment
    ) {
        log.info(
                "Creating Razorpay payment link for paymentId={}",
                payment.getId()
        );

        // Obtain the amount from the validated payment request.
        BigDecimal amount = paymentRequest.amount();

        // Create a payment link using the Razorpay SDK.
        PaymentLink razorpayPaymentLink = createRazorpayLink(
                amount,
                payment.getId()
        );

        // Save the provider's payment link ID for later reconciliation.
        payment.setPaymentLinkId(
                razorpayPaymentLink.get("id")
        );

        // Convert the provider response into our application DTO.
        PaymentLinkResponse response =
                paymentMapper.toLinkRazorPayResponse(
                        razorpayPaymentLink
                );

        log.info(
                "Razorpay payment link created for paymentId={}",
                payment.getId()
        );

        return response;
    }

    /**
     * Creates a payment link using the Razorpay Payment Link API.
     *
     * @param amount payment amount in the application's currency
     * @param paymentId internal payment identifier
     * @return Razorpay payment link response
     */
    private PaymentLink createRazorpayLink(
            BigDecimal amount,
            Object paymentId
    ) {
        // Razorpay expects the amount in the smallest currency unit.
        // For INR, convert rupees into paise.
        // Example: INR 15.00 becomes 1500 paise.
        BigDecimal razorpayAmount = amount
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY);

        JSONObject paymentLinkRequest = new JSONObject();

        // Set the amount in paise.
        paymentLinkRequest.put(
                "amount",
                razorpayAmount.longValueExact()
        );

        // Set the supported currency.
        paymentLinkRequest.put(
                "currency",
                "INR"
        );

        // Add a human-readable payment description.
        paymentLinkRequest.put(
                "description",
                "Payment for payment #" + paymentId
        );

        // Use our internal payment ID for reconciliation.
        paymentLinkRequest.put(
                "reference_id",
                String.valueOf(paymentId)
        );

        // Enable email and SMS notifications.
        JSONObject notify = new JSONObject();
        notify.put("email", true);
        notify.put("sms", true);

        paymentLinkRequest.put("notify", notify);

        // Allow Razorpay to send automated reminders.
        paymentLinkRequest.put("reminder_enable", true);

        // Configure the frontend callback URL.
        // Replace this with your actual frontend URL in production.
        paymentLinkRequest.put(
                "callback_url",
                "http://localhost:3000/api/payments/razorpay/callback"
        );

        paymentLinkRequest.put(
                "callback_method",
                "get"
        );

        try {
            return razorpayClient.paymentLink.create(
                    paymentLinkRequest
            );
        } catch (RazorpayException exception) {
            log.error(
                    "Failed to create Razorpay payment link for paymentId={}",
                    paymentId,
                    exception
            );

            throw new IllegalStateException(
                    "Unable to create Razorpay payment link",
                    exception
            );
        }
    }
}
