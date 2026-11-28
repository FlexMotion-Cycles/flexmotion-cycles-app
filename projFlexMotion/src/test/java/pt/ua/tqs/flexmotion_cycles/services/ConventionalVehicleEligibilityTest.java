package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.ua.tqs.flexmotion_cycles.data.MaintenanceTicketRepository;
import pt.ua.tqs.flexmotion_cycles.data.TripRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.TicketStatus;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleStatus;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@ExtendWith(MockitoExtension.class)
class ConventionalVehicleEligibilityTest {

    @Mock
    private MaintenanceTicketRepository maintenanceTicketRepository;

    @Mock
    private TripRepository tripRepository;

    private ConventionalVehicleEligibility eligibility;
    private Station central;

    @BeforeEach
    void setUp() {
        eligibility = new ConventionalVehicleEligibility(maintenanceTicketRepository, tripRepository);
        central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
    }

    @ParameterizedTest
    @EnumSource(value = VehicleType.class, names = {"E_BIKE", "CARGO_E_BIKE"})
    @Requirement("FM-29")
    void availableConventionalVehicleAtMinimumBatteryIsEligible(VehicleType type) {
        Vehicle vehicle = new Vehicle("EB-01", type, 20, central);

        assertThat(eligibility.isEligible(vehicle)).isTrue();
    }

    @Test
    @Requirement("FM-29")
    void vehicleBelowMinimumBatteryIsNotEligible() {
        Vehicle vehicle = new Vehicle("EB-04", VehicleType.E_BIKE, 19, central);

        assertThat(eligibility.isEligible(vehicle)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = VehicleStatus.class,
            names = {"RESERVED", "IN_USE", "MAINTENANCE", "REMOVED"})
    @Requirement("FM-29")
    void vehicleThatIsNotAvailableIsNotEligible(VehicleStatus status) {
        Vehicle vehicle = new Vehicle("EB-05", VehicleType.E_BIKE, 80, central);
        ReflectionTestUtils.setField(vehicle, "status", status);

        assertThat(eligibility.isEligible(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-29")
    void vehicleWithOpenTicketIsNotEligible() {
        Vehicle vehicle = new Vehicle("EB-03", VehicleType.E_BIKE, 80, central);
        when(maintenanceTicketRepository.existsByVehicleAndStatus(vehicle, TicketStatus.OPEN))
                .thenReturn(true);

        assertThat(eligibility.isEligible(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-29")
    void accessibleVehicleIsNotEligible() {
        Vehicle vehicle = new Vehicle("WB-02", VehicleType.WHEELCHAIR_BIKE, 90, central);

        assertThat(eligibility.isEligible(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-29")
    void vehicleWithPendingUnlockIsNotEligible() {
        Vehicle vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        when(tripRepository.existsByVehicleAndStatus(vehicle, TripStatus.UNLOCK_REQUESTED))
                .thenReturn(true);

        assertThat(eligibility.isEligible(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-29")
    void reservedVehicleIsEligibleWhenReserved() {
        Vehicle vehicle = reservedVehicle();

        assertThat(eligibility.isEligibleWhenReserved(vehicle)).isTrue();
    }

    @Test
    @Requirement("FM-29")
    void availableVehicleIsNotEligibleWhenReserved() {
        Vehicle vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);

        assertThat(eligibility.isEligibleWhenReserved(vehicle)).isFalse();
    }

    @Test
    @Requirement("FM-29")
    void reservedVehicleWithOpenTicketIsNotEligibleWhenReserved() {
        Vehicle vehicle = reservedVehicle();
        when(maintenanceTicketRepository.existsByVehicleAndStatus(vehicle, TicketStatus.OPEN))
                .thenReturn(true);

        assertThat(eligibility.isEligibleWhenReserved(vehicle)).isFalse();
    }

    private Vehicle reservedVehicle() {
        Vehicle vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        ReflectionTestUtils.setField(vehicle, "status", VehicleStatus.RESERVED);
        return vehicle;
    }
}
