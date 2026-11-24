package pt.ua.tqs.flexmotion_cycles.services;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.DuplicateVehicleCodeException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.InvalidStateTransitionException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationFullException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vehicle service (US-03, US-04)")
class VehicleServiceTest {

    private static final Long STATION_ID = 1L;
    private static final Long VEHICLE_ID = 1L;
    private static final Long UNKNOWN_ID = 99L;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    @Requirement("FM-21")
    void registersNewVehicleAsAvailableAtItsStation() {
        Station station = givenStationWithDocks(20, 0);
        when(vehicleRepository.save(any(Vehicle.class))).then(returnsFirstArg());

        Vehicle vehicle = vehicleService.registerVehicle(eBike("EB-0001"));

        assertThat(vehicle.getState()).isEqualTo(VehicleState.AVAILABLE);
        assertThat(vehicle.getCode()).isEqualTo("EB-0001");
        assertThat(vehicle.getType()).isEqualTo(VehicleType.E_BIKE);
        assertThat(vehicle.getStation()).isSameAs(station);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @Requirement("FM-21")
    void acceptsVehicleWhenStationHasOneFreeDock() {
        givenStationWithDocks(20, 19);
        when(vehicleRepository.save(any(Vehicle.class))).then(returnsFirstArg());

        Vehicle vehicle = vehicleService.registerVehicle(eBike("EB-0020"));

        assertThat(vehicle.getState()).isEqualTo(VehicleState.AVAILABLE);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleWhenStationIsFull() {
        givenStationWithDocks(20, 20);
        VehicleRequest request = eBike("EB-0021");

        assertThatThrownBy(() -> vehicleService.registerVehicle(request))
                .isInstanceOf(StationFullException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleAtUnknownStation() {
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());
        VehicleRequest request = eBike("EB-0001");

        assertThatThrownBy(() -> vehicleService.registerVehicle(request))
                .isInstanceOf(StationNotFoundException.class);
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleWithAlreadyRegisteredCode() {
        givenStationWithDocks(20, 0);
        when(vehicleRepository.existsByCode("EB-0001")).thenReturn(true);
        VehicleRequest request = eBike("EB-0001");

        assertThatThrownBy(() -> vehicleService.registerVehicle(request))
                .isInstanceOf(DuplicateVehicleCodeException.class);
        verify(vehicleRepository, never()).save(any());
    }

    /**
     * Prepares a saved station with the given capacity and number of docked vehicles. The
     * stubs are lenient because a request refused early never looks at the station.
     */
    private Station givenStationWithDocks(int capacity, long docked) {
        Station station = Station.conventional("Central Square",
                new GeoPoint(40.6405, -8.6538), capacity, 10.0);
        ReflectionTestUtils.setField(station, "id", STATION_ID);
        lenient().when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));
        lenient().when(vehicleRepository.countByStationId(STATION_ID)).thenReturn(docked);
        return station;
    }

    private static VehicleRequest eBike(String code) {
        return new VehicleRequest(code, VehicleType.E_BIKE, STATION_ID);
    }

    // ---------- US-04: vehicle lifecycle ----------

    private Vehicle givenVehicleIn(VehicleState state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, null);
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

        assertThrows(InvalidStateTransitionException.class,
                () -> vehicleService.changeState(VEHICLE_ID, VehicleState.REMOVED));
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

        assertThrows(InvalidStateTransitionException.class,
                () -> vehicleService.deactivate(VEHICLE_ID));
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

        assertThrows(InvalidStateTransitionException.class,
                () -> vehicleService.activate(VEHICLE_ID));
        assertEquals(VehicleState.AVAILABLE, vehicle.getState());

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("Changing the state of an unknown vehicle throws not found")
    void changeStateUnknownVehicleThrowsNotFound() {
        givenUnknownVehicle();

        assertThrows(VehicleNotFoundException.class,
                () -> vehicleService.changeState(UNKNOWN_ID, VehicleState.IN_USE));

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
