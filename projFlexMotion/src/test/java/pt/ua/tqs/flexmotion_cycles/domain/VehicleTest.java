package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VehicleTest {

    private Station central;
    private Station north;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        north = new Station("North", 40.6500, -8.6500, 10, StationType.CONVENTIONAL);
        vehicle = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
    }

    @Test
    @Requirement("FM-28")
    void vehicleInUseBecomesAvailableAtTheStationWhereItWasLocked() {
        vehicle.startUse();

        vehicle.endUse(north);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(vehicle.getStation()).isSameAs(north);
    }

    @ParameterizedTest
    @EnumSource(value = VehicleState.class, names = "IN_USE", mode = EnumSource.Mode.EXCLUDE)
    @Requirement("FM-28")
    void vehicleNotInUseCannotEndUse(VehicleState state) {
        vehicle.setState(state);

        assertThatThrownBy(() -> vehicle.endUse(north)).isInstanceOf(IllegalStateException.class);
        assertThat(vehicle.getState()).isEqualTo(state);
        assertThat(vehicle.getStation()).isSameAs(central);
    }

    @Test
    @Requirement("FM-28")
    void vehicleCannotEndUseWithoutAStation() {
        vehicle.startUse();

        assertThatThrownBy(() -> vehicle.endUse(null)).isInstanceOf(NullPointerException.class);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    }
}