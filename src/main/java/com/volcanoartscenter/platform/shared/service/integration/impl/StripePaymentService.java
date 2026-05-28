package com.volcanoartscenter.platform.shared.service.integration.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Product;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.volcanoartscenter.platform.shared.service.integration.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripePaymentService implements PaymentGatewayService {

    private final String secretKey;
    private final String publishableKey;
    private final String webhookSecret;

    public StripePaymentService(@Value("${platform.integrations.stripe.secret-key:}") String secretKey,
                                @Value("${platform.integrations.stripe.publishable-key:}") String publishableKey,
                                @Value("${platform.integrations.stripe.webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey;
        this.publishableKey = publishableKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public String provider() { return "STRIPE_CARD"; }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public String publishableKey() {
        return publishableKey;
    }

    public String webhookSecret() {
        return webhookSecret;
    }

    @Override
    public PaymentResult initialize(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        CardIntent intent = initializeCardIntent(reference, amount, currency, metadata);
        return new PaymentResult(true, intent.id(), "Stripe payment intent created");
    }

    public CardIntent initializeCardIntent(String reference, BigDecimal amount, String currency, Map<String, String> metadata) {
        ensureConfigured();
        Stripe.apiKey = secretKey;

        long minorUnits = amount.multiply(new BigDecimal("100")).longValueExact();
        Map<String, String> stripeMetadata = new HashMap<>();
        stripeMetadata.put("reference", reference);
        if (metadata != null) stripeMetadata.putAll(metadata);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(minorUnits)
                .setCurrency(currency.toLowerCase())
                .setDescription("VolcanoArts:" + reference)
                .putAllMetadata(stripeMetadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .build();
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return new CardIntent(intent.getId(), intent.getClientSecret(), intent.getStatus());
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe initialization failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PaymentResult verify(String externalReference) {
        ensureConfigured();
        Stripe.apiKey = secretKey;
        try {
            PaymentIntent intent = PaymentIntent.retrieve(externalReference);
            String status = intent.getStatus();
            boolean success = "succeeded".equalsIgnoreCase(status) || "requires_capture".equalsIgnoreCase(status);
            return new PaymentResult(success, externalReference, "Stripe status: " + status);
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe verification failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Creates a Stripe Customer + a Product + a recurring Subscription. Returns
     * the subscription id, the customer id, and (best-effort) the
     * {@code clientSecret} of the latest unpaid invoice's payment intent — the
     * browser confirms that to complete the first charge.
     *
     * <p>Subsequent renewal charges arrive as {@code invoice.paid} webhooks.
     */
    public SubscriptionHandle createDonationSubscription(
            String customerEmail, String customerName,
            BigDecimal amount, String currency, String interval, String reference) {
        ensureConfigured();
        Stripe.apiKey = secretKey;
        long minorUnits = amount.multiply(new BigDecimal("100")).longValueExact();
        SubscriptionCreateParams.Item.PriceData.Recurring.Interval recurring = switch (interval == null ? "month" : interval.toLowerCase()) {
            case "year", "annually", "annual" -> SubscriptionCreateParams.Item.PriceData.Recurring.Interval.YEAR;
            case "week", "weekly" -> SubscriptionCreateParams.Item.PriceData.Recurring.Interval.WEEK;
            case "day", "daily" -> SubscriptionCreateParams.Item.PriceData.Recurring.Interval.DAY;
            default -> SubscriptionCreateParams.Item.PriceData.Recurring.Interval.MONTH;
        };
        long intervalCount = "quarter".equalsIgnoreCase(interval) || "quarterly".equalsIgnoreCase(interval) ? 3 : 1;

        try {
            // 1. Create the Stripe Product (newer SDK no longer accepts inline ProductData on Subscription PriceData)
            Product product = Product.create(ProductCreateParams.builder()
                    .setName("Volcano Arts Center donation: " + reference)
                    .putMetadata("reference", reference)
                    .build());

            // 2. Create the Customer
            Customer customer = Customer.create(CustomerCreateParams.builder()
                    .setEmail(customerEmail)
                    .setName(customerName == null ? "Donor" : customerName)
                    .putMetadata("reference", reference)
                    .build());

            // 3. Create the Subscription with inline price referencing that Product
            SubscriptionCreateParams subParams = SubscriptionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPriceData(SubscriptionCreateParams.Item.PriceData.builder()
                                    .setCurrency(currency.toLowerCase())
                                    .setProduct(product.getId())
                                    .setUnitAmount(minorUnits)
                                    .setRecurring(SubscriptionCreateParams.Item.PriceData.Recurring.builder()
                                            .setInterval(recurring)
                                            .setIntervalCount(intervalCount)
                                            .build())
                                    .build())
                            .build())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                            .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                            .build())
                    .addAllExpand(List.of("latest_invoice.payment_intent", "latest_invoice.confirmation_secret"))
                    .putMetadata("reference", reference)
                    .build();

            Subscription subscription = Subscription.create(subParams);
            String clientSecret = extractClientSecret(subscription);
            return new SubscriptionHandle(subscription.getId(), customer.getId(),
                    clientSecret, subscription.getStatus());
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe subscription failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Best-effort client-secret extraction across Stripe Java versions. Tries
     * the type-safe Invoice.payment_intent path first; if that getter has been
     * removed (recent API versions deprecate it), falls back to
     * Subscription.confirmation_secret via reflection. Returns null rather than
     * throwing — the donation row is still created, and the frontend can
     * exchange the subscription id for a fresh client secret if needed.
     */
    private String extractClientSecret(Subscription subscription) {
        try {
            Invoice invoice = subscription.getLatestInvoiceObject();
            // Path A: invoice.getPaymentIntentObject() (older Stripe Java versions)
            String secret = tryReflective(invoice, "getPaymentIntentObject", "getClientSecret");
            if (secret != null) return secret;
            // Path B: invoice.getPaymentIntent() returns a String id we can retrieve
            String paymentIntentId = tryReflective(invoice, "getPaymentIntent");
            if (paymentIntentId != null && !paymentIntentId.isBlank()) {
                try {
                    PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);
                    return pi.getClientSecret();
                } catch (StripeException ignored) { /* fall through */ }
            }
            // Path C: subscription.getConfirmationSecret().getClientSecret() (newest Stripe API)
            secret = tryReflective(subscription, "getConfirmationSecret", "getClientSecret");
            if (secret != null) return secret;
        } catch (Exception ignored) { /* swallow — null is acceptable */ }
        return null;
    }

    /** Invokes obj.method() (no args) and returns its String value, or null. */
    private static String tryReflective(Object obj, String methodName) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object result = m.invoke(obj);
            return result == null ? null : result.toString();
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    /** Invokes obj.first().second() (no args) and returns String, or null. */
    private static String tryReflective(Object obj, String first, String second) {
        if (obj == null) return null;
        try {
            Method m1 = obj.getClass().getMethod(first);
            Object intermediate = m1.invoke(obj);
            if (intermediate == null) return null;
            Method m2 = intermediate.getClass().getMethod(second);
            Object result = m2.invoke(intermediate);
            return result == null ? null : result.toString();
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Stripe integration is not configured");
        }
    }

    public record CardIntent(String id, String clientSecret, String status) {}
    public record SubscriptionHandle(String subscriptionId, String customerId,
                                     String clientSecret, String status) {}
}
