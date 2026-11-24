package pt.ua.tqs.flexmotion_cycles.domain;

import java.math.BigDecimal;
/**
 * Cost breakdown of a finished trip (BR6, BR7, BR17).
 *
 * @param unlockFee fixed unlock fee
 * @param billedMinutes started minutes of use, including overtime minutes
 * @param overtimeMinutes started minutes beyond the maximum trip duration
 * @param usageCost minutes up to the maximum duration at the per-minute rate
 * @param overtimeCost overtime minutes at the overtime rate
 * @param reservationFee fee of the reservation used by the trip, zero without one
 * @param total sum of all the above
 */
public record TripCost(BigDecimal unlockFee, long billedMinutes, long overtimeMinutes, BigDecimal usageCost, BigDecimal overtimeCost, BigDecimal reservationFee, BigDecimal total) {
    
}
