package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.MaintenanceTicket;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class VehicleRepositoryIT {

    private static final int MIN_BATTERY_LEVEL = 20;
    private static final List<VehicleType> CONVENTIONAL_TYPES =
            List.of(VehicleType.E_BIKE, VehicleType.CARGO_E_BIKE);

    private final VehicleRepository vehicleRepository;
    private final TestEntityManager entityManager;

    private Station central;

    @Autowired
    VehicleRepositoryIT(VehicleRepository vehicleRepository, TestEntityManager entityManager) {
        this.vehicleRepository = vehicleRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        central = entityManager.persist(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
    }

    @Test
    @Requirement("FM-27")
    void listsAvailableEbikeAtMinimumBattery() {
        persistVehicle("EB-01", VehicleType.E_BIKE, 20);

        assertThat(findEligibleCodes()).containsExactly("EB-01");
    }

    @Test
    @Requirement("FM-27")
    void excludesEbikeBelowMinimumBattery() {
        persistVehicle("EB-01", VehicleType.E_BIKE, 19);

        assertThat(findEligibleCodes()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class, names = {"RESERVED", "IN_USE", "MAINTENANCE"})
    @Requirement("FM-27")
    void excludesVehiclesThatAreNotAvailable(VehicleStatus status) {
        Vehicle vehicle = persistVehicle("EB-02", VehicleType.E_BIKE, 80);
        forceStatus(vehicle, status);

        assertThat(findEligibleCodes()).isEmpty();
    }

    @Test
    @Requirement("FM-27")
    void excludesVehicleWithOpenTicket() {
        Vehicle vehicle = persistVehicle("EB-03", VehicleType.E_BIKE, 80);
        entityManager.persist(new MaintenanceTicket(vehicle, Instant.parse("2026-11-22T09:00:00Z")));

        assertThat(findEligibleCodes()).isEmpty();
    }

    @Test
    @Requirement("FM-27")
    void excludesAccessibleVehicles() {
        persistVehicle("WB-01", VehicleType.WHEELCHAIR_BIKE, 90);

        assertThat(findEligibleCodes()).isEmpty();
    }

    @Test
    @Requirement("FM-27")
    void countsOnlyVehiclesThatOccupyADock() {
        persistVehicle("EB-01", VehicleType.E_BIKE, 80);
        forceStatus(persistVehicle("EB-02", VehicleType.E_BIKE, 80), VehicleStatus.MAINTENANCE);
        forceStatus(persistVehicle("EB-03", VehicleType.E_BIKE, 80), VehicleStatus.IN_USE);

        long docked = vehicleRepository.countByStationAndStatusNotIn(central,
                EnumSet.of(VehicleStatus.IN_USE, VehicleStatus.REMOVED));

        assertThat(docked).isEqualTo(2);
    }

    private Vehicle persistVehicle(String code, VehicleType type, int batteryLevel) {
        return entityManager.persist(new Vehicle(code, type, batteryLevel, central));
    }

    /** Test fixture only: lifecycle transitions belong to US-04 and are not exposed yet. */
    private void forceStatus(Vehicle vehicle, VehicleStatus status) {
        entityManager.flush();
        entityManager.getEntityManager()
                .createQuery("UPDATE Vehicle v SET v.status = :status WHERE v.id = :id")
                .setParameter("status", status)
                .setParameter("id", vehicle.getId())
                .executeUpdate();
        entityManager.clear();
    }

    private List<String> findEligibleCodes() {
        return vehicleRepository
                .findAvailableWithoutOpenTicket(MIN_BATTERY_LEVEL, CONVENTIONAL_TYPES)
                .stream()
                .map(Vehicle::getCode)
                .toList();
    }
}
