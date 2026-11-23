package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@ExtendWith(MockitoExtension.class)
class StationAvailabilityServiceTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private StationAvailabilityService service;

    @Test
    @Requirement("FM-27")
    void searchesWithMinimumBatteryOfTwentyPercent() {
        service.findConventionalStations();

        verify(vehicleRepository).findAvailableWithoutOpenTicket(eq(20), anyCollection());
    }

    @Test
    @Requirement("FM-27")
    void searchesOnlyConventionalVehicleTypes() {
        service.findConventionalStations();

        verify(vehicleRepository).findAvailableWithoutOpenTicket(anyInt(),
                eq(List.of(VehicleType.E_BIKE, VehicleType.CARGO_E_BIKE)));
    }

    @Test
    @Requirement("FM-27")
    void listsOnlyConventionalStations() {
        service.findConventionalStations();

        verify(stationRepository).findByType(StationType.CONVENTIONAL);
    }

    @Test
    @Requirement("FM-27")
    void groupsAvailableEbikesByStation() {
        Station central = station(1L, "Central", 10);
        Station campus = station(2L, "Campus", 5);
        Vehicle eb1 = new Vehicle("EB-01", VehicleType.E_BIKE, 50, central);
        Vehicle eb2 = new Vehicle("EB-02", VehicleType.E_BIKE, 80, campus);
        when(stationRepository.findByType(StationType.CONVENTIONAL))
                .thenReturn(List.of(central, campus));
        when(vehicleRepository.findAvailableWithoutOpenTicket(anyInt(), anyCollection()))
                .thenReturn(List.of(eb1, eb2));

        List<StationAvailability> result = service.findConventionalStations();

        assertThat(result).extracting(StationAvailability::availableVehicles)
                .containsExactly(List.of(eb1), List.of(eb2));
    }

    @Test
    @Requirement("FM-27")
    void freeDocksAreCapacityMinusDockedVehicles() {
        Station central = station(1L, "Central", 10);
        when(stationRepository.findByType(StationType.CONVENTIONAL)).thenReturn(List.of(central));
        when(vehicleRepository.countByStationAndStatusNotIn(eq(central), any())).thenReturn(3L);

        List<StationAvailability> result = service.findConventionalStations();

        assertThat(result.get(0).freeDocks()).isEqualTo(7);
    }

    @Test
    @Requirement("FM-27")
    void stationWithoutAvailableEbikesIsStillListed() {
        Station central = station(1L, "Central", 10);
        when(stationRepository.findByType(StationType.CONVENTIONAL)).thenReturn(List.of(central));

        List<StationAvailability> result = service.findConventionalStations();

        assertThat(result).singleElement()
                .satisfies(availability -> assertThat(availability.availableVehicles()).isEmpty());
    }

    private static Station station(Long id, String name, int capacity) {
        Station station = new Station(name, 40.64, -8.65, capacity, StationType.CONVENTIONAL);
        ReflectionTestUtils.setField(station, "id", id);
        return station;
    }
}
