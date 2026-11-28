package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class PaymentMethodTest {

    @Test
    @Requirement("FM-32")
    void keepsOnlyTheGatewayTokenAndTheLastFourDigits() {
        PaymentMethod card = new PaymentMethod("ines", "pm_token_123", "4242");

        assertThat(card.getUsername()).isEqualTo("ines");
        assertThat(card.getGatewayToken()).isEqualTo("pm_token_123");
        assertThat(card.getLast4()).isEqualTo("4242");
    }

    @Test
    @Requirement("FM-32")
    void aNewCardIsNotTheDefaultUntilMadeDefault() {
        PaymentMethod card = new PaymentMethod("ines", "pm_token_123", "4242");
        assertThat(card.isDefault()).isFalse();

        card.makeDefault();
        assertThat(card.isDefault()).isTrue();

        card.clearDefault();
        assertThat(card.isDefault()).isFalse();
    }

    @ParameterizedTest
    @Requirement("FM-32")
    @ValueSource(strings = {"424", "42424", "42a2", "4242424242424242"})
    void refusesAnythingButExactlyFourDigits(String last4) {
        assertThatThrownBy(() -> new PaymentMethod("ines", "pm_token_123", last4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @Requirement("FM-32")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void refusesACardWithoutAGatewayToken(String token) {
        assertThatThrownBy(() -> new PaymentMethod("ines", token, "4242"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @Requirement("FM-32")
    @NullAndEmptySource
    void refusesACardWithoutAnOwner(String username) {
        assertThatThrownBy(() -> new PaymentMethod(username, "pm_token_123", "4242"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
