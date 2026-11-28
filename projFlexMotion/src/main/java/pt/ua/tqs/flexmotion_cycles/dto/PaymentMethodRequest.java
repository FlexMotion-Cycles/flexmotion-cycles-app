package pt.ua.tqs.flexmotion_cycles.dto;

import jakarta.validation.constraints.NotBlank;

/** Card token created by the payment sandbox on the client; card data never reaches the API. */
public record PaymentMethodRequest(@NotBlank String cardToken) {
}
