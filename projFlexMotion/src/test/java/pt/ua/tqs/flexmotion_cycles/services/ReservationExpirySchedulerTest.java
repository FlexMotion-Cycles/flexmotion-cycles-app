package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import pt.ua.tqs.flexmotion_cycles.domain.Reservation;

@ExtendWith(MockitoExtension.class)
class ReservationExpirySchedulerTest {

    @Mock
    private ReservationService reservationService;

    private ReservationExpiryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationExpiryScheduler(reservationService);
    }

    @Test
    @Requirement("FM-29")
    void expiresEveryDueReservation() {
        when(reservationService.findDueReservationIds()).thenReturn(List.of(1L, 2L));

        scheduler.expireDueReservations();

        verify(reservationService).expireIfDue(1L);
        verify(reservationService).expireIfDue(2L);
    }

    @Test
    @Requirement("FM-29")
    void keepsExpiringAfterAConcurrentChange() {
        when(reservationService.findDueReservationIds()).thenReturn(List.of(1L, 2L));
        when(reservationService.expireIfDue(1L))
                .thenThrow(new ObjectOptimisticLockingFailureException(Reservation.class, 1L));

        assertThatCode(() -> scheduler.expireDueReservations()).doesNotThrowAnyException();

        verify(reservationService).expireIfDue(2L);
    }
}
