package pt.ua.tqs.flexmotion_cycles.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** BR5: periodically expires the reservations whose hold has ended. */
@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${flexmotion.reservation.expiry-check-interval}")
    public void expireDueReservations() {
        for (Long reservationId : reservationService.findDueReservationIds()) {
            try {
                reservationService.expireIfDue(reservationId);
            } catch (OptimisticLockingFailureException exception) {
                // An unlock changed it first; the next check sees its new state.
                log.info("Reservation {} changed concurrently, checked again next run",
                        reservationId);
            }
        }
    }
}
