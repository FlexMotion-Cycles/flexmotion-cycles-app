package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-30T07:45:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-30T08:00:00Z");
    private static final Instant LAST_SECOND = EXPIRES_AT.minusSeconds(1);
    private static final Duration HOLD = Duration.ofMinutes(15);
    private static final BigDecimal FEE = new BigDecimal("0.50");

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        Vehicle vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        reservation = new Reservation("ines", vehicle, CREATED_AT, HOLD, FEE);
    }

    @Test
    @Requirement("FM-29")
    void newReservationIsActiveForTheHoldWithItsFee() {
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getUsername()).isEqualTo("ines");
        assertThat(reservation.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(reservation.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(reservation.getFee()).isEqualByComparingTo(FEE);
    }

    @Test
    @Requirement("FM-29")
    void reservationIsFulfilledOneSecondBeforeTheHoldEnds() {
        reservation.fulfil(LAST_SECOND);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
    }

    @Test
    @Requirement("FM-29")
    void reservationCannotBeFulfilledWhenTheHoldEnds() {
        assertThatThrownBy(() -> reservation.fulfil(EXPIRES_AT)).isInstanceOf(IllegalStateException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @Requirement("FM-29")
    void reservationExpiresWhenTheHoldEnds() {
        reservation.expire(EXPIRES_AT);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @Requirement("FM-29")
    void reservationCannotExpireBeforeTheHoldEnds() {
        assertThatThrownBy(() -> reservation.expire(LAST_SECOND)).isInstanceOf(IllegalStateException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @Requirement("FM-29")
    void fulfilledReservationCannotExpire() {
        reservation.fulfil(LAST_SECOND);

        assertThatThrownBy(() -> reservation.expire(EXPIRES_AT)).isInstanceOf(IllegalStateException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
    }

    @Test
    @Requirement("FM-29")
    void expiredReservationCannotBeFulfilled() {
        reservation.expire(EXPIRES_AT);

        assertThatThrownBy(() -> reservation.fulfil(LAST_SECOND)).isInstanceOf(IllegalStateException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
}
