package pt.ua.tqs.flexmotion_cycles.services;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.config.PricingProperties;
import pt.ua.tqs.flexmotion_cycles.domain.TripCost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TripCostCalculatorTest {
    private static final PricingProperties PRICING = new PricingProperties(new BigDecimal("1.00"), new BigDecimal("0.15"), new BigDecimal("0.30"), 90);
    private static final BigDecimal NO_RESERVATION = BigDecimal.ZERO;
    private TripCostCalculator calculator;

    @BeforeEach 
    void setUp() {
        calculator = new TripCostCalculator(PRICING);
    }

    @ParameterizedTest(name = "{0} s -> {1} min, {2} overtime, total {3}")
    @CsvSource({
        "0,    0,  0, 1.00",
        "59,   1,  0, 1.15",
        "60,   1,  0, 1.15",
        "61,   2,  0, 1.30",
        "5400, 90, 0, 14.50",
        "5401, 91, 1, 14.80"
    })
    @Requirement("FM-28")
    void startedMinuteCountsAsFullMinuteAndOvertimeStartsAfterMaxDuration(long seconds,long billedMinutes, long overtimeMinutes, String total) {
        TripCost cost = calculator.calculate(Duration.ofSeconds(seconds), NO_RESERVATION);

        assertThat(cost.billedMinutes()).isEqualTo(billedMinutes);
        assertThat(cost.overtimeMinutes()).isEqualTo(overtimeMinutes);
        assertThat(cost.total()).isEqualByComparingTo(total);
    }

    @Test
    @Requirement("FM-28")
    void partialSecondStillStartsANewMinute() {
        TripCost cost = calculator.calculate(Duration.ofMillis(60_001), NO_RESERVATION);

        assertThat(cost.billedMinutes()).isEqualTo(2);
    }

    @Test
    @Requirement("FM-28")
    void costBreakdownSeparatesUnlockUsageAndOvertime() {
        TripCost cost = calculator.calculate(Duration.ofMinutes(92), NO_RESERVATION);

        assertThat(cost.unlockFee()).isEqualByComparingTo("1.00");
        assertThat(cost.usageCost()).isEqualByComparingTo("13.50");
        assertThat(cost.overtimeCost()).isEqualByComparingTo("0.60");
        assertThat(cost.reservationFee()).isEqualByComparingTo("0.00");
        assertThat(cost.total()).isEqualByComparingTo("15.10");
    }

    @Test
    @Requirement("FM-28")
    void reservationFeeIsAddedToTheTotalWhenTheTripUsedAReservation() {
        TripCost cost = calculator.calculate(Duration.ofSeconds(60), new BigDecimal("0.50"));

        assertThat(cost.reservationFee()).isEqualByComparingTo("0.50");
        assertThat(cost.total()).isEqualByComparingTo("1.65");
    }

    @Test
    @Requirement("FM-28")
    void totalHasTwoDecimalPlaces() {
        TripCost cost = calculator.calculate(Duration.ofSeconds(60), NO_RESERVATION);

        assertThat(cost.total().scale()).isEqualTo(2);
    }

    @Test
    @Requirement("FM-28")
    void negativeDurationIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(Duration.ofSeconds(-1), NO_RESERVATION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
