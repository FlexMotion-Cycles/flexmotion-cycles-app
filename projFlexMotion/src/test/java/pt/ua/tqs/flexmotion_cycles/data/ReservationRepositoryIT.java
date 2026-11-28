package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReservationRepositoryIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-30T07:45:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-30T08:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(15);
    private static final BigDecimal FEE = new BigDecimal("0.50");
    private static final String OWNER = "ines";
    private static final String OTHER_USER = "rui";

    private final ReservationRepository reservationRepository;
    private final TripRepository tripRepository;
    private final TestEntityManager entityManager;

    private Vehicle ebike;
    private Vehicle otherEbike;

    @Autowired
    ReservationRepositoryIT(ReservationRepository reservationRepository,
            TripRepository tripRepository, TestEntityManager entityManager) {
        this.reservationRepository = reservationRepository;
        this.tripRepository = tripRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        Station central = entityManager.persist(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
        ebike = entityManager.persist(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
        otherEbike = entityManager.persist(new Vehicle("EB-02", VehicleType.E_BIKE, 80, central));
    }

    @Test
    @Requirement("FM-29")
    void savesReservationWithItsHoldAndFee() {
        Long id = entityManager.persistAndGetId(reservationOf(OWNER, ebike), Long.class);
        entityManager.flush();
        entityManager.clear();

        Reservation found = reservationRepository.findById(id).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(found.getUsername()).isEqualTo(OWNER);
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(found.getFee()).isEqualTo(FEE);
    }

    @Test
    @Requirement("FM-29")
    void findsActiveReservationOfTheVehicle() {
        Reservation reservation = entityManager.persist(reservationOf(OWNER, ebike));
        entityManager.persist(reservationOf(OTHER_USER, otherEbike));

        assertThat(reservationRepository.findByVehicleAndStatus(ebike, ReservationStatus.ACTIVE))
                .contains(reservation);
    }

    @Test
    @Requirement("FM-29")
    void ignoresReservationOfTheVehicleThatIsNoLongerActive() {
        Reservation expired = reservationOf(OWNER, ebike);
        expired.expire(EXPIRES_AT);
        entityManager.persist(expired);

        assertThat(reservationRepository.findByVehicleAndStatus(ebike, ReservationStatus.ACTIVE))
                .isEmpty();
    }

    @Test
    @Requirement("FM-29")
    void findsActiveReservationsOfTheUserOnly() {
        Reservation active = entityManager.persist(reservationOf(OWNER, ebike));
        entityManager.persist(reservationOf(OTHER_USER, otherEbike));
        Reservation expired = reservationOf(OWNER, otherEbike);
        expired.expire(EXPIRES_AT);
        entityManager.persist(expired);

        assertThat(reservationRepository.findByUsernameAndStatus(OWNER, ReservationStatus.ACTIVE))
                .containsExactly(active);
    }

    @Test
    @Requirement("FM-29")
    void reservationIsDueExactlyWhenTheHoldEnds() {
        Reservation reservation = entityManager.persist(reservationOf(OWNER, ebike));

        assertThat(reservationRepository.findByStatusAndExpiresAtLessThanEqual(
                ReservationStatus.ACTIVE, EXPIRES_AT)).containsExactly(reservation);
    }

    @Test
    @Requirement("FM-29")
    void reservationIsNotDueOneSecondBeforeTheHoldEnds() {
        entityManager.persist(reservationOf(OWNER, ebike));

        assertThat(reservationRepository.findByStatusAndExpiresAtLessThanEqual(
                ReservationStatus.ACTIVE, EXPIRES_AT.minusSeconds(1))).isEmpty();
    }

    @Test
    @Requirement("FM-29")
    void dueQueryIgnoresReservationsThatAreNoLongerActive() {
        Reservation fulfilled = reservationOf(OWNER, ebike);
        fulfilled.fulfil(CREATED_AT);
        entityManager.persist(fulfilled);

        assertThat(reservationRepository.findByStatusAndExpiresAtLessThanEqual(
                ReservationStatus.ACTIVE, EXPIRES_AT)).isEmpty();
    }

    @Test
    @Requirement("FM-29")
    void ownerFindsTheReservationWithItsVehicle() {
        Long id = entityManager.persistAndGetId(reservationOf(OWNER, ebike), Long.class);
        entityManager.flush();
        entityManager.clear();

        Reservation found = reservationRepository.findByIdAndUsername(id, OWNER).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getVehicle())).isTrue();
        assertThat(found.getVehicle().getCode()).isEqualTo("EB-01");
    }

    @Test
    @Requirement("FM-29")
    void otherUserCannotFindTheReservation() {
        Long id = entityManager.persistAndGetId(reservationOf(OWNER, ebike), Long.class);

        assertThat(reservationRepository.findByIdAndUsername(id, OTHER_USER)).isEmpty();
    }

    @Test
    @Requirement("FM-29")
    void reservationCanBeFulfilledByOnlyOneTrip() {
        Reservation reservation = entityManager.persist(reservationOf(OWNER, ebike));
        tripRepository.saveAndFlush(new Trip(OWNER, ebike, CREATED_AT, reservation));
        Trip duplicate = new Trip(OWNER, ebike, CREATED_AT.plusSeconds(60), reservation);

        assertThatThrownBy(() -> tripRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Reservation reservationOf(String username, Vehicle vehicle) {
        return new Reservation(username, vehicle, CREATED_AT, HOLD, FEE);
    }
}
