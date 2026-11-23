package pt.ua.tqs.flexmotion_cycles.boundary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StationControllerIT {

    private static final String STATIONS_URL = "/api/v1/stations";

    private static final String CONVENTIONAL_STATION = """
            {
              "name": "Central Square",
              "latitude": 40.6405,
              "longitude": -8.6538,
              "type": "CONVENTIONAL",
              "dockCapacity": 20
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @BeforeEach
    void cleanDatabase() {
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
    }

    @Test
    @Requirement("FM-21")
    void operatorRegistersConventionalStationWithOverflowZone() throws Exception {
        mockMvc.perform(post(STATIONS_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONVENTIONAL_STATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Central Square"))
                .andExpect(jsonPath("$.latitude").value(40.6405))
                .andExpect(jsonPath("$.longitude").value(-8.6538))
                .andExpect(jsonPath("$.type").value("CONVENTIONAL"))
                .andExpect(jsonPath("$.dockCapacity").value(20))
                .andExpect(jsonPath("$.overflowRadiusMeters").value(10.0));
    }

    @Test
    @Requirement("FM-21")
    void operatorRegistersAccessibleHubWithTechnicianHours() throws Exception {
        String accessibleHub = """
                {
                  "name": "Riverside Hub",
                  "latitude": 40.6420,
                  "longitude": -8.6500,
                  "type": "ACCESSIBLE",
                  "dockCapacity": 6,
                  "stepFreeAccess": true,
                  "technicianFrom": "09:00",
                  "technicianTo": "18:00"
                }
                """;

        mockMvc.perform(post(STATIONS_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accessibleHub))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ACCESSIBLE"))
                .andExpect(jsonPath("$.stepFreeAccess").value(true))
                .andExpect(jsonPath("$.technicianFrom").value("09:00:00"))
                .andExpect(jsonPath("$.technicianTo").value("18:00:00"))
                .andExpect(jsonPath("$.overflowRadiusMeters").doesNotExist());
    }

    @Test
    @Requirement("FM-21")
    void refusesStationWithZeroDocks() throws Exception {
        String zeroDocks = """
                {
                  "name": "Central Square",
                  "latitude": 40.6405,
                  "longitude": -8.6538,
                  "type": "CONVENTIONAL",
                  "dockCapacity": 0
                }
                """;

        mockMvc.perform(post(STATIONS_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroDocks))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dock capacity must be at least 1"));
    }

    @Test
    @Requirement("FM-21")
    void refusesStationWithoutNameOrWithLatitudeOutOfRange() throws Exception {
        String invalidStation = """
                {
                  "name": "",
                  "latitude": 91.0,
                  "longitude": -8.6538,
                  "type": "CONVENTIONAL",
                  "dockCapacity": 20
                }
                """;

        mockMvc.perform(post(STATIONS_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidStation))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Requirement("FM-21")
    void forbidsEndUserFromRegisteringStation() throws Exception {
        mockMvc.perform(post(STATIONS_URL)
                        .with(user("ines").roles("END_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONVENTIONAL_STATION))
                .andExpect(status().isForbidden());
    }

    @Test
    @Requirement("FM-21")
    void requiresAuthenticationToRegisterStation() throws Exception {
        mockMvc.perform(post(STATIONS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONVENTIONAL_STATION))
                .andExpect(status().isUnauthorized());
    }
}