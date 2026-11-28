package pt.ua.tqs.flexmotion_cycles.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties("flexmotion.payment")
public record PaymentProperties(@NotNull @Positive BigDecimal depositAmount,
        @NotBlank String currency) {
}
