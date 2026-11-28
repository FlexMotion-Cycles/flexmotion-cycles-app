package pt.ua.tqs.flexmotion_cycles.config;

import java.math.BigDecimal;
import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Reservation of a conventional vehicle: exclusive hold and fixed fee (BR5, BR6). */
@Validated
@ConfigurationProperties("flexmotion.reservation")
public record ReservationProperties(
        @NotNull @DurationMin(seconds = 1) Duration hold,
        @NotNull @PositiveOrZero BigDecimal fee) {
}
