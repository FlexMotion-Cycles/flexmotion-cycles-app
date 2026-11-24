package pt.ua.tqs.flexmotion_cycles.boundary;

import java.math.BigDecimal;

import pt.ua.tqs.flexmotion_cycles.domain.TripCost;

public record TripCostDto(BigDecimal unlockFee, long billedMinutes, long overtimeMinutes, BigDecimal usageCost, BigDecimal overtimeCost, BigDecimal reservationFee, BigDecimal total) {
    static TripCostDto from(TripCost cost) {
        if (cost == null) {
            return null;
        }
        return new TripCostDto(cost.unlockFee(), cost.billedMinutes(), cost.overtimeMinutes(), cost.usageCost(), cost.overtimeCost(), cost.reservationFee(), cost.total());
    }
}
