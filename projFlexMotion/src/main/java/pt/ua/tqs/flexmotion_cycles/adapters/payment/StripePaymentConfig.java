package pt.ua.tqs.flexmotion_cycles.adapters.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import pt.ua.tqs.flexmotion_cycles.config.PaymentProperties;

/** Uses the Stripe sandbox as the payment gateway when flexmotion.payment.adapter=stripe. */
@Configuration
@ConditionalOnProperty(name = "flexmotion.payment.adapter", havingValue = "stripe")
@EnableConfigurationProperties(StripeProperties.class)
class StripePaymentConfig {

    @Bean
    StripePaymentAdapter stripePaymentAdapter(StripeProperties stripe, PaymentProperties payment) {
        return new StripePaymentAdapter(stripeClient(RestClient.builder(), stripe),
                payment.currency());
    }

    /** Client for the Stripe REST API, authenticated with the secret key as a bearer token. */
    static RestClient stripeClient(RestClient.Builder builder, StripeProperties stripe) {
        if (stripe.secretKey() == null || stripe.secretKey().isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_SECRET_KEY must be set");
        }
        return builder.baseUrl(stripe.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + stripe.secretKey())
                .build();
    }
}
