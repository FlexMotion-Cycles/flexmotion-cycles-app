package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.ReservationRepository;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.ReservationStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** The version check happens on commit, so this test manages its own transactions. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationConcurrencyIT {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final TransactionTemplate transaction;
    private final TransactionTemplate concurrentTransaction;

    @Autowired
    ReservationConcurrencyIT(ReservationService reservationService,
            ReservationRepository reservationRepository, VehicleRepository vehicleRepository,
            StationRepository stationRepository, PlatformTransactionManager transactionManager) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @BeforeEach
    void setUp() {
        Station central = stationRepository.save(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
        vehicleRepository.save(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
    }

    /** This test commits, so it must not leave data for the other tests on this database. */
    @AfterEach
    void cleanUp() {
        reservationRepository.deleteAll();
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
    }

    @Test
    @Requirement("FM-29")
    void onlyOneOfTwoConcurrentReservationsOfTheSameVehicleSucceeds() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(first -> {
            reservationService.reserve("ines", "EB-01");
            // Another user reserves the same e-bike and commits first.
            concurrentTransaction.executeWithoutResult(
                    second -> reservationService.reserve("rui", "EB-01"));
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(reservationRepository.findAll()).singleElement()
                .satisfies(reservation -> {
                    assertThat(reservation.getUsername()).isEqualTo("rui");
                    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
                });
        assertThat(vehicleRepository.findByCode("EB-01").orElseThrow().getStatus())
                .isEqualTo(VehicleStatus.RESERVED);
    }
}
