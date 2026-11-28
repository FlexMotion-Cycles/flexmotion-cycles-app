package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Stripe sandbox access: the API base URL (with its version) and the test secret key. */
@ConfigurationProperties(prefix = "flexmotion.payment.stripe")
record StripeProperties(String baseUrl, String secretKey) {
}
