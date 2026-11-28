package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.PaymentGatewayException;

/**
 * Payment port over the Stripe REST API in test mode (sandbox only, no real money). Paths are
 * relative to the configured base URL. A 402 answer is a card declined by the bank; any other
 * error means the gateway could not answer.
 */
public class StripePaymentAdapter implements PaymentPort {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() { };
    /** Stripe amounts are integers in the smallest currency unit (cents for EUR). */
    private static final int CENT_DECIMALS = 2;

    private static final String REQUIRES_CAPTURE = "requires_capture";
    private static final String SUCCEEDED = "succeeded";
    private static final String CANCELED = "canceled";
    private static final String PENDING = "pending";
    private static final String MANUAL_CAPTURE = "manual";
    private static final String AUTOMATIC_CAPTURE = "automatic";

    private final RestClient stripe;
    private final String currency;

    public StripePaymentAdapter(RestClient stripe, String currency) {
        this.stripe = stripe;
        this.currency = currency;
    }

    @Override
    public CardLinkResult linkCard(String cardToken) {
        if (cardToken == null || cardToken.isBlank()) {
            return CardLinkResult.declined();
        }
        String customerId = text(post("/customers", new LinkedMultiValueMap<>())
                .orElseThrow(() -> unexpected("creating a customer")), "id");
        return post("/payment_methods/{id}/attach", form("customer", customerId), cardToken)
                .map(card -> CardLinkResult.approved(text(card, "id"), last4(card)))
                .orElseGet(CardLinkResult::declined);
    }

    @Override
    public PaymentResult authorize(String gatewayToken, BigDecimal amount) {
        return createPaymentIntent(gatewayToken, amount, MANUAL_CAPTURE, REQUIRES_CAPTURE);
    }

    @Override
    public PaymentResult capture(String gatewayRef, BigDecimal amount) {
        return post("/payment_intents/{id}/capture",
                form("amount_to_capture", cents(amount)), gatewayRef)
                .filter(intent -> SUCCEEDED.equals(intent.get("status")))
                .map(intent -> PaymentResult.approved(gatewayRef))
                .orElseGet(PaymentResult::declined);
    }

    @Override
    public PaymentResult release(String gatewayRef) {
        return post("/payment_intents/{id}/cancel", new LinkedMultiValueMap<>(), gatewayRef)
                .filter(intent -> CANCELED.equals(intent.get("status")))
                .map(intent -> PaymentResult.approved(gatewayRef))
                .orElseGet(PaymentResult::declined);
    }

    @Override
    public PaymentResult charge(String gatewayToken, BigDecimal amount) {
        return createPaymentIntent(gatewayToken, amount, AUTOMATIC_CAPTURE, SUCCEEDED);
    }

    @Override
    public PaymentResult refund(String gatewayRef, BigDecimal amount) {
        MultiValueMap<String, String> body = form("payment_intent", gatewayRef);
        body.add("amount", cents(amount));
        return post("/refunds", body)
                .filter(refund -> SUCCEEDED.equals(refund.get("status"))
                        || PENDING.equals(refund.get("status")))
                .map(refund -> PaymentResult.approved(text(refund, "id")))
                .orElseGet(PaymentResult::declined);
    }

    /** A card attached to a customer can only be charged together with that customer. */
    private PaymentResult createPaymentIntent(String gatewayToken, BigDecimal amount,
            String captureMethod, String expectedStatus) {
        String customerId = text(get("/payment_methods/{id}", gatewayToken), "customer");
        MultiValueMap<String, String> body = form("amount", cents(amount));
        body.add("currency", currency);
        body.add("customer", customerId);
        body.add("payment_method", gatewayToken);
        body.add("payment_method_types[]", "card");
        body.add("capture_method", captureMethod);
        body.add("confirm", "true");
        body.add("off_session", "true");
        return post("/payment_intents", body)
                .filter(intent -> expectedStatus.equals(intent.get("status")))
                .map(intent -> PaymentResult.approved(text(intent, "id")))
                .orElseGet(PaymentResult::declined);
    }

    /** Empty when Stripe declines the card (402). */
    private Optional<Map<String, Object>> post(String path, MultiValueMap<String, String> body,
            Object... uriVariables) {
        return send(stripe.post().uri(path, uriVariables)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body));
    }

    private Map<String, Object> get(String path, Object... uriVariables) {
        return send(stripe.get().uri(path, uriVariables))
                .orElseThrow(() -> unexpected("reading " + path));
    }

    private Optional<Map<String, Object>> send(RestClient.RequestHeadersSpec<?> request) {
        try {
            return request.exchange((sent, response) -> {
                if (response.getStatusCode().value() == HttpStatus.PAYMENT_REQUIRED.value()) {
                    return Optional.empty();
                }
                if (response.getStatusCode().isError()) {
                    throw new PaymentGatewayException(
                            "Stripe answered " + response.getStatusCode().value());
                }
                return Optional.ofNullable(response.bodyTo(JSON_OBJECT));
            });
        } catch (RestClientException exception) {
            throw new PaymentGatewayException("Stripe could not be reached", exception);
        }
    }

    private static MultiValueMap<String, String> form(String name, String value) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(name, value);
        return body;
    }

    /** Refuses fractions of a cent instead of silently rounding money. */
    static String cents(BigDecimal amount) {
        try {
            return Long.toString(amount.movePointRight(CENT_DECIMALS).longValueExact());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount " + amount + " has fractions of a cent",
                    exception);
        }
    }

    private static String text(Map<String, Object> json, String field) {
        Object value = json.get(field);
        if (value == null) {
            throw unexpected("missing field " + field);
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static String last4(Map<String, Object> paymentMethod) {
        Object card = paymentMethod.get("card");
        if (!(card instanceof Map<?, ?>)) {
            throw unexpected("payment method without card details");
        }
        return text((Map<String, Object>) card, "last4");
    }

    private static PaymentGatewayException unexpected(String what) {
        return new PaymentGatewayException("Unexpected Stripe answer: " + what);
    }
}
