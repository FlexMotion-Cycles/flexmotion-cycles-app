package pt.ua.tqs.flexmotion_cycles.dto;

import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;

/** A linked card as the user sees it: only the last four digits, never the gateway token. */
public record PaymentMethodResponse(Long id, String last4, boolean isDefault) {

    public static PaymentMethodResponse from(PaymentMethod card) {
        return new PaymentMethodResponse(card.getId(), card.getLast4(), card.isDefault());
    }
}
