package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.MaintenanceTicket;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TicketOrigin;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MaintenanceTicketRepositoryIT {

    private static final Instant OPENED_AT = Instant.parse("2026-11-22T16:05:00Z");

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final TestEntityManager entityManager;

    private Vehicle vehicle;
    private Vehicle otherVehicle;

    @Autowired
    MaintenanceTicketRepositoryIT(MaintenanceTicketRepository maintenanceTicketRepository,
            TestEntityManager entityManager) {
        this.maintenanceTicketRepository = maintenanceTicketRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        Station station = entityManager.persist(
                new Station("Telemetry Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
        vehicle = entityManager.persist(new Vehicle("EB-0001", VehicleType.E_BIKE, station));
        otherVehicle = entityManager.persist(new Vehicle("EB-0002", VehicleType.E_BIKE, station));
    }

    @Test
    @Requirement("FM-26")
    void findsOpenFaultTicketOfTheVehicle() {
        entityManager.persist(MaintenanceTicket.forFaultEvent(vehicle, "BRAKE_FAILURE", OPENED_AT));

        assertThat(openFaultTicketExists(vehicle)).isTrue();
    }

    @Test
    @Requirement("FM-26")
    void ignoresOpenTicketOfAnotherOrigin() {
        entityManager.persist(new MaintenanceTicket(vehicle, TicketOrigin.STAFF, OPENED_AT));

        assertThat(openFaultTicketExists(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-26")
    void ignoresFaultTicketOfAnotherVehicle() {
        entityManager.persist(
                MaintenanceTicket.forFaultEvent(otherVehicle, "BRAKE_FAILURE", OPENED_AT));

        assertThat(openFaultTicketExists(vehicle)).isFalse();
    }

    private boolean openFaultTicketExists(Vehicle target) {
        return maintenanceTicketRepository.existsByVehicleAndOriginAndStatus(
                target, TicketOrigin.FAULT_EVENT, TicketStatus.OPEN);
    }
}
