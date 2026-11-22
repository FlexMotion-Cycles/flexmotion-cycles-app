package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** The version check happens on commit, so this test manages its own transactions. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class VehicleLockingIT {

    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final TransactionTemplate transaction;
    private final TransactionTemplate concurrentTransaction;

    @Autowired
    VehicleLockingIT(VehicleRepository vehicleRepository, StationRepository stationRepository,
            PlatformTransactionManager transactionManager) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.concurrentTransaction = new TransactionTemplate(transactionManager);
        concurrentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        Station central = stationRepository.save(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
        vehicleRepository.save(new Vehicle("EB-01", VehicleType.E_BIKE, 80, central));
    }

    @Test
    @Requirement("FM-27")
    void secondConcurrentUnlockOfTheSameVehicleFails() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(first -> {
            vehicleRepository.findLockedByCode("EB-01");
            // Another unlock of the same vehicle commits first.
            concurrentTransaction.executeWithoutResult(
                    second -> vehicleRepository.findLockedByCode("EB-01"));
        })).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
