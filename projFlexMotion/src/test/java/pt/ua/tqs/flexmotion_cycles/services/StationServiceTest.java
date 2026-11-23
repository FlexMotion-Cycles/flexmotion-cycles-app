package pt.ua.tqs.flexmotion_cycles.services;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.dto.StationRequest;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.InvalidStationException;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    private static final LocalTime NINE_AM = LocalTime.of(9, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationService stationService;

    @Test
    @Requirement("FM-21")
    void refusesStationWithZeroDocks() {
        StationRequest request = conventionalStation(0);

        assertThatThrownBy(() -> stationService.registerStation(request))
                .isInstanceOf(InvalidStationException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void acceptsStationWithOneDock() {
        when(stationRepository.save(any(Station.class))).then(returnsFirstArg());

        Station station = stationService.registerStation(conventionalStation(1));

        assertThat(station.getDockCapacity()).isEqualTo(1);
        verify(stationRepository).save(station);
    }

    @Test
    @Requirement("FM-21")
    void givesConventionalStationAnOverflowZoneOfTenMetres() {
        when(stationRepository.save(any(Station.class))).then(returnsFirstArg());

        Station station = stationService.registerStation(conventionalStation(20));

        assertThat(station.getType()).isEqualTo(StationType.CONVENTIONAL);
        assertThat(station.getName()).isEqualTo("Central Square");
        assertThat(station.getLocation()).isEqualTo(new GeoPoint(40.6405, -8.6538));
        assertThat(station.getOverflowRadiusMeters()).isEqualTo(10.0);
        assertThat(station.getTechnicianFrom()).isNull();
    }

    @Test
    @Requirement("FM-21")
    void registersAccessibleHubWithStepFreeAccessAndTechnicianHours() {
        when(stationRepository.save(any(Station.class))).then(returnsFirstArg());

        Station station = stationService.registerStation(accessibleHub(true, NINE_AM, SIX_PM));

        assertThat(station.getType()).isEqualTo(StationType.ACCESSIBLE);
        assertThat(station.getStepFreeAccess()).isTrue();
        assertThat(station.getTechnicianFrom()).isEqualTo(NINE_AM);
        assertThat(station.getTechnicianTo()).isEqualTo(SIX_PM);
        assertThat(station.getOverflowRadiusMeters()).isNull();
    }

    @Test
    @Requirement("FM-21")
    void refusesAccessibleHubWithoutStepFreeAccessInformation() {
        StationRequest request = accessibleHub(null, NINE_AM, SIX_PM);

        assertThatThrownBy(() -> stationService.registerStation(request))
                .isInstanceOf(InvalidStationException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void refusesAccessibleHubWithoutTechnicianStartTime() {
        StationRequest request = accessibleHub(true, null, SIX_PM);

        assertThatThrownBy(() -> stationService.registerStation(request))
                .isInstanceOf(InvalidStationException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void refusesAccessibleHubWithoutTechnicianEndTime() {
        StationRequest request = accessibleHub(true, NINE_AM, null);

        assertThatThrownBy(() -> stationService.registerStation(request))
                .isInstanceOf(InvalidStationException.class);
        verify(stationRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-21")
    void refusesAccessibleHubWhoseTechnicianHoursDoNotEndAfterTheyStart() {
        StationRequest request = accessibleHub(true, NINE_AM, NINE_AM);

        assertThatThrownBy(() -> stationService.registerStation(request))
                .isInstanceOf(InvalidStationException.class);
        verify(stationRepository, never()).save(any());
    }

    private static StationRequest conventionalStation(int docks) {
        return new StationRequest("Central Square", 40.6405, -8.6538,
                StationType.CONVENTIONAL, docks, null, null, null);
    }

    private static StationRequest accessibleHub(Boolean stepFreeAccess, LocalTime from,
            LocalTime to) {
        return new StationRequest("Marginal Accessible Hub", 40.6405, -8.6538,
                StationType.ACCESSIBLE, 6, stepFreeAccess, from, to);
    }
}