package pt.ua.tqs.flexmotion_cycles.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

import org.springframework.stereotype.Component;
import pt.ua.tqs.flexmotion_cycles.config.PricingProperties;
import pt.ua.tqs.flexmotion_cycles.domain.TripCost;

@Component 
public class TripCostCalculator {
    private static final int MONEY_SCALE = 2;
    private final PricingProperties pricing;
    public TripCostCalculator(PricingProperties pricing) {
        this.pricing = pricing;
    }

    public TripCost calculate(Duration duration, BigDecimal reservationFee) {
        Objects.requireNonNull(reservationFee, "reservationFee");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Trip duration cannot be negative: " + duration);
        }

        long billedMinutes = startedMinutes(duration);
        long overtimeMinutes = Math.max(0, billedMinutes - pricing.maxTripMinutes());
        long normalMinutes = billedMinutes - overtimeMinutes;

        BigDecimal unlockFee = money(pricing.unlockFee());
        BigDecimal usageCost = money(pricing.perMinuteRate().multiply(BigDecimal.valueOf(normalMinutes)));
        BigDecimal overtimeCost = money(pricing.overtimeRate().multiply(BigDecimal.valueOf(overtimeMinutes)));
        BigDecimal reservation = money(reservationFee);
        BigDecimal total = unlockFee.add(usageCost).add(overtimeCost).add(reservation);

        return new TripCost(unlockFee, billedMinutes, overtimeMinutes, usageCost, overtimeCost, reservation, total);
    }

    private static long startedMinutes(Duration duration) {
        long fullminutes = duration.toMinutes();
        boolean partialminutes = duration.compareTo(Duration.ofMinutes(fullminutes)) > 0;
        return partialminutes ? fullminutes + 1 : fullminutes;
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
