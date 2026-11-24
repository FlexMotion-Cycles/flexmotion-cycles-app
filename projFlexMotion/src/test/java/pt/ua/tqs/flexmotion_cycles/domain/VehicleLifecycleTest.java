package pt.ua.tqs.flexmotion_cycles.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.InvalidStateTransitionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Vehicle lifecycle (US-04)")
class VehicleLifecycleTest {

    private VehicleLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = new VehicleLifecycle();
    }

    private static Vehicle vehicleIn(VehicleState state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, null);
        vehicle.setState(state);
        return vehicle;
    }

    @Requirement("FM-22")
    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Valid transitions are accepted")
    @CsvSource({
        "AVAILABLE,   RESERVED",
        "AVAILABLE,   IN_USE",
        "AVAILABLE,   MAINTENANCE",
        "AVAILABLE,   REMOVED",
        "RESERVED,    IN_USE",
        "RESERVED,    AVAILABLE",
        "IN_USE,      AVAILABLE",
        "MAINTENANCE, AVAILABLE",
        "MAINTENANCE, REMOVED"
    })
    void validTransitionIsAccepted(VehicleState fromState, VehicleState toState) {
        Vehicle vehicle = vehicleIn(fromState);

        assertTrue(lifecycle.isAllowed(fromState, toState));
        lifecycle.apply(vehicle, toState);

        assertEquals(toState, vehicle.getState());
    }

    @Requirement("FM-22")
    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Invalid transitions are rejected and the state does not change")
    @CsvSource({
        "AVAILABLE,   AVAILABLE",
        "RESERVED,    RESERVED",
        "RESERVED,    MAINTENANCE",
        "RESERVED,    REMOVED",
        "IN_USE,      IN_USE",
        "IN_USE,      MAINTENANCE",
        "MAINTENANCE, MAINTENANCE",
        "MAINTENANCE, RESERVED",
        "MAINTENANCE, IN_USE"
    })
    void invalidTransitionIsRejected(VehicleState fromState, VehicleState toState) {
        Vehicle vehicle = vehicleIn(fromState);

        assertFalse(lifecycle.isAllowed(fromState, toState));
        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> lifecycle.apply(vehicle, toState));

        assertEquals(fromState, vehicle.getState());
        assertEquals(fromState, ex.getFrom());
        assertEquals(toState, ex.getTo());
    }

    @Requirement("FM-22")
    @ParameterizedTest(name = "REMOVED -> {0}")
    @DisplayName("REMOVED is final")
    @EnumSource(VehicleState.class)
    void removedIsFinal(VehicleState toState) {
        Vehicle vehicle = vehicleIn(VehicleState.REMOVED);

        assertFalse(lifecycle.isAllowed(VehicleState.REMOVED, toState));
        assertThrows(InvalidStateTransitionException.class,
                () -> lifecycle.apply(vehicle, toState));
        assertEquals(VehicleState.REMOVED, vehicle.getState());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("A vehicle IN_USE cannot be reserved")
    void vehicleInUseCannotBeReserved() {
        Vehicle vehicle = vehicleIn(VehicleState.IN_USE);

        assertThrows(InvalidStateTransitionException.class,
                () -> lifecycle.apply(vehicle, VehicleState.RESERVED));
        assertEquals(VehicleState.IN_USE, vehicle.getState());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("A vehicle IN_USE cannot be removed")
    void vehicleInUseCannotBeRemoved() {
        Vehicle vehicle = vehicleIn(VehicleState.IN_USE);

        assertThrows(InvalidStateTransitionException.class,
                () -> lifecycle.apply(vehicle, VehicleState.REMOVED));
        assertEquals(VehicleState.IN_USE, vehicle.getState());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Operator deactivates an AVAILABLE vehicle (moves to MAINTENANCE)")
    void operatorDeactivatesAvailableVehicle() {
        Vehicle vehicle = vehicleIn(VehicleState.AVAILABLE);

        lifecycle.deactivate(vehicle);

        assertEquals(VehicleState.MAINTENANCE, vehicle.getState());
    }

    @Requirement("FM-22")
    @ParameterizedTest(name = "from {0}")
    @DisplayName("Deactivate is rejected when the vehicle is not AVAILABLE")
    @EnumSource(value = VehicleState.class, names = "AVAILABLE", mode = EnumSource.Mode.EXCLUDE)
    void deactivateIsRejectedWhenNotAvailable(VehicleState fromState) {
        Vehicle vehicle = vehicleIn(fromState);

        assertThrows(InvalidStateTransitionException.class, () -> lifecycle.deactivate(vehicle));
        assertEquals(fromState, vehicle.getState());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Operator activates a vehicle in MAINTENANCE (back to AVAILABLE)")
    void operatorActivatesVehicleInMaintenance() {
        Vehicle vehicle = vehicleIn(VehicleState.MAINTENANCE);

        lifecycle.activate(vehicle);

        assertEquals(VehicleState.AVAILABLE, vehicle.getState());
    }

    @Requirement("FM-22")
    @ParameterizedTest(name = "from {0}")
    @DisplayName("Activate is rejected when the vehicle is not in MAINTENANCE")
    @EnumSource(value = VehicleState.class, names = "MAINTENANCE", mode = EnumSource.Mode.EXCLUDE)
    void activateIsRejectedWhenNotInMaintenance(VehicleState fromState) {
        Vehicle vehicle = vehicleIn(fromState);

        assertThrows(InvalidStateTransitionException.class, () -> lifecycle.activate(vehicle));
        assertEquals(fromState, vehicle.getState());
    }
}