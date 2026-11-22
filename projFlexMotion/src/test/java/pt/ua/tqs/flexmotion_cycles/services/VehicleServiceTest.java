package pt.ua.tqs.flexmotion_cycles.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.domain.exceptions.InvalidStateTransitionException;
import pt.ua.tqs.flexmotion_cycles.domain.exceptions.VehicleNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vehicle service (US-04)")
public class VehicleServiceTest {
    
    private static final Long VEHICLE_ID = 1L;
    private static final Long UNKNOWN_ID = 99L;

    @Mock 
    private VehicleRepository vehicleRepository;

    @InjectMocks 
    private VehicleService vehicleService;

    private Vehicle givenVehicleIn(VehicleState state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, 80, true);
        vehicle.setState(state);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private void givenUnknownVehicle() {
        when(vehicleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());
    }

    @Test 
    @Requirement("FM-22")
    @DisplayName("Valid state change is applied and saved")
    void changeStateValidTransitionIsSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.AVAILABLE);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        Vehicle result = vehicleService.changeState(VEHICLE_ID, VehicleState.IN_USE);

        assertEquals(vehicle, result);
        assertEquals(VehicleState.IN_USE, result.getState());

        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Invalid state change is rejected and nothing is saved")
    void changeStateInvalidTransitionisNotSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.IN_USE);

        assertThrows(InvalidStateTransitionException.class, () -> vehicleService.changeState(VEHICLE_ID, VehicleState.REMOVED));
        assertEquals(VehicleState.IN_USE, vehicle.getState());

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Operator deactivates an AVAILABLE vehicle and it is saved")
    void deactivateAvailableVehicleIsSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.AVAILABLE);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        Vehicle result = vehicleService.deactivate(VEHICLE_ID);

        assertEquals(VehicleState.MAINTENANCE, result.getState());

        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Deactivating a vehicle IN_USE is rejected and nothing is saved")
    void deactivateVehicleInUseIsNotSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.IN_USE);

        assertThrows(InvalidStateTransitionException.class, () -> vehicleService.deactivate(VEHICLE_ID));
        assertEquals(VehicleState.IN_USE, vehicle.getState());

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Operator activates a vehicle in MAINTENANCE and it is saved")
    void activateVehicleInMaintenanceIsSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.MAINTENANCE);
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        Vehicle result = vehicleService.activate(VEHICLE_ID);

        assertEquals(VehicleState.AVAILABLE, result.getState());

        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Activating an AVAILABLE vehicle is rejected and nothing is saved")
    void activateAvailableVehicleIsNotSaved() {
        Vehicle vehicle = givenVehicleIn(VehicleState.AVAILABLE);

        assertThrows(InvalidStateTransitionException.class, () -> vehicleService.activate(VEHICLE_ID));
        assertEquals(VehicleState.AVAILABLE, vehicle.getState());

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Changing the state of an unknown vehicle throws not found")
    void changeStateUnknownVehicleThrowsNotFound() {
        givenUnknownVehicle();

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.changeState(UNKNOWN_ID, VehicleState.IN_USE));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Deactivating an unknown vehicle throws not found")
    void deactivateUnkwnownVehicleThrowsNotFound() {
        givenUnknownVehicle();

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.deactivate(UNKNOWN_ID));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Activating an unknown vehicle throws not found")
    void activateUnknownVehicleThrowsNotFound() {
        givenUnknownVehicle();

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.activate(UNKNOWN_ID));

        verify(vehicleRepository, never()).save(any());
    }
}
