package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.services.StationAvailability;
import pt.ua.tqs.flexmotion_cycles.services.StationAvailabilityService;

@WebMvcTest(StationController.class)
@Import(SecurityConfig.class)
class StationControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private StationAvailabilityService stationAvailabilityService;

    @Autowired
    StationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @Requirement("FM-27")
    void listsStationsWithFreeDocksAndEbikesWithoutAuthentication() throws Exception {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 20, central);
        when(stationAvailabilityService.findConventionalStations())
                .thenReturn(List.of(new StationAvailability(central, 7, List.of(ebike))));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Central"))
                .andExpect(jsonPath("$[0].freeDocks").value(7))
                .andExpect(jsonPath("$[0].availableEbikes[0].code").value("EB-01"))
                .andExpect(jsonPath("$[0].availableEbikes[0].batteryLevel").value(20));
    }

    @Test
    @Requirement("FM-27")
    void stationWithoutAvailableEbikesShowsAnEmptyList() throws Exception {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        when(stationAvailabilityService.findConventionalStations())
                .thenReturn(List.of(new StationAvailability(central, 10, List.of())));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableEbikes").isEmpty());
    }
}
