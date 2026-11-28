package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-11-22T10:00:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-11-22T10:00:05Z");
    private static final Instant ENDED_AT = Instant.parse("2026-11-22T10:20:05Z");
    private static final TripCost COST = new TripCost(new BigDecimal("1.00"), 20, 0, new BigDecimal("3.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("4.00"));

    private Station north;
    private Vehicle vehicle;
    private Trip trip;

    @BeforeEach
    void setUp() {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        north = new Station("North", 40.6500, -8.6500, 10, StationType.CONVENTIONAL);
        vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        trip = new Trip("ines", vehicle, REQUESTED_AT);
    }

    @Test
    @Requirement("FM-29")
    void tripFromAReservationIsLinkedToIt() {
        Reservation reservation = new Reservation("ines", vehicle, REQUESTED_AT.minusSeconds(300),
                Duration.ofMinutes(15), new BigDecimal("0.50"));

        Trip reservedTrip = new Trip("ines", vehicle, REQUESTED_AT, reservation);

        assertThat(reservedTrip.getReservation()).isSameAs(reservation);
        assertThat(reservedTrip.getStatus()).isEqualTo(TripStatus.UNLOCK_REQUESTED);
    }

    @Test
    @Requirement("FM-29")
    void immediateTripHasNoReservation() {
        assertThat(trip.getReservation()).isNull();
    }

    @Test
    @Requirement("FM-28")
    void activeTripEndsWithItsTimeStationAndCost() {
        trip.start(STARTED_AT);

        trip.end(ENDED_AT, north, COST);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.ENDED);
        assertThat(trip.getEndedAt()).isEqualTo(ENDED_AT);
        assertThat(trip.getEndStation()).isSameAs(north);
        assertThat(trip.getCost()).isEqualTo(COST);
    }

    @Test
    @Requirement("FM-28")
    void costIsUnknownWhileTheTripIsActive() {
        trip.start(STARTED_AT);

        assertThat(trip.getCost()).isNull();
        assertThat(trip.getEndedAt()).isNull();
    }

    @Test
    @Requirement("FM-28")
    void tripThatHasNotStartedCannotEnd() {
        assertThatThrownBy(() -> trip.end(ENDED_AT, north, COST)).isInstanceOf(IllegalStateException.class);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.UNLOCK_REQUESTED);
    }

    @Test
    @Requirement("FM-28")
    void endedTripCannotEndAgain() {
        trip.start(STARTED_AT);
        trip.end(ENDED_AT, north, COST);

        assertThatThrownBy(() -> trip.end(ENDED_AT.plusSeconds(60), north, COST)).isInstanceOf(IllegalStateException.class);
        assertThat(trip.getEndedAt()).isEqualTo(ENDED_AT);
    }

    @Test
    @Requirement("FM-28")
    void tripCannotEndBeforeItStarted() {
        trip.start(STARTED_AT);

        assertThatThrownBy(() -> trip.end(STARTED_AT.minusSeconds(1), north, COST)).isInstanceOf(IllegalArgumentException.class);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.ACTIVE);
    }
}