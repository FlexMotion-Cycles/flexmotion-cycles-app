package pt.ua.tqs.flexmotion_cycles.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@Import(TestcontainersConfiguration.class)
@DataJpaTest 
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Vehicle Repository IT (US-04)")
public class VehicleRepositoryIT {
    
    private final VehicleRepository vehicleRepository;
    private final TestEntityManager entityManager;

    @Autowired 
    public VehicleRepositoryIT(VehicleRepository vehicleRepository, TestEntityManager entityManager) {
        this.vehicleRepository = vehicleRepository;
        this.entityManager = entityManager;
    }

    private Vehicle persistNewVehicle() {
        Vehicle vehicle = entityManager.persistFlushFind(new Vehicle("EB-001", VehicleType.E_BIKE, 80, true));
        entityManager.clear();
        return vehicle;
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("A saved vehicle is found with its state")
    void savedVehicleIsFoundWithItsState() {
        Vehicle saved = persistNewVehicle();
        Vehicle found = vehicleRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getId());
        assertEquals(VehicleState.AVAILABLE, found.getState());
        assertEquals(0L, found.getVersion());
    }
    
    @Test
    @Requirement("FM-22")
    @DisplayName("A state change is persisted and increments the version")
    void stateChangeIsPersistedAndIncrementsVersion() {
        Vehicle saved = persistNewVehicle();
        Vehicle found = vehicleRepository.findById(saved.getId()).orElseThrow();

        found.setState(VehicleState.MAINTENANCE);
        vehicleRepository.saveAndFlush(found);
        entityManager.clear();

        Vehicle reloaded = vehicleRepository.findById(found.getId()).orElseThrow();
        
        assertEquals(VehicleState.MAINTENANCE, reloaded.getState());
        assertTrue(reloaded.getVersion() > 0L);
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("A concurrent state change on a stale copy is rejected (optimistic locking)")
    void concurrentStateChangeIsRejected() {
        Long id = persistNewVehicle().getId();

        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow();
        entityManager.detach(vehicle);

        Vehicle currVehicle = vehicleRepository.findById(id).orElseThrow();
        currVehicle.setState(VehicleState.IN_USE);
        vehicleRepository.saveAndFlush(currVehicle);

        vehicle.setState(VehicleState.MAINTENANCE);

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> vehicleRepository.saveAndFlush(vehicle));
        
    }
}
