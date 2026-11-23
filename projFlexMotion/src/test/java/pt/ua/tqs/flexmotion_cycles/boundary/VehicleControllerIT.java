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
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleControllerIT {

    private static final String VEHICLES_URL = "/api/v1/vehicles";
    private static final GeoPoint VALE_CLARO_CENTRE = new GeoPoint(40.6405, -8.6538);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private Station centralSquare;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAll();
        stationRepository.deleteAll();
        centralSquare = stationRepository.save(
                Station.conventional("Central Square", VALE_CLARO_CENTRE, 2, 10.0));
    }

    @Test
    @Requirement("FM-21")
    void operatorRegistersVehicleAsAvailableAtItsStation() throws Exception {
        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("EB-0001", "E_BIKE", centralSquare.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("EB-0001"))
                .andExpect(jsonPath("$.type").value("E_BIKE"))
                .andExpect(jsonPath("$.state").value("AVAILABLE"))
                .andExpect(jsonPath("$.stationId").value(centralSquare.getId()));
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleWhenStationIsFull() throws Exception {
        vehicleRepository.save(new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));
        vehicleRepository.save(new Vehicle("EB-0002", VehicleType.E_BIKE, centralSquare));

        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("EB-0003", "E_BIKE", centralSquare.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Station " + centralSquare.getId() + " has no free dock"));
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleAtUnknownStation() throws Exception {
        long unknownStationId = centralSquare.getId() + 1000;

        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("EB-0001", "E_BIKE", unknownStationId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "Station " + unknownStationId + " does not exist"));
    }

    @Test
    @Requirement("FM-21")
    void refusesVehicleWithAlreadyRegisteredCode() throws Exception {
        vehicleRepository.save(new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));

        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("EB-0001", "CARGO_E_BIKE", centralSquare.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "A vehicle with code EB-0001 is already registered"));
    }

    @Test
    @Requirement("FM-21")
    void refusesUnknownVehicleType() throws Exception {
        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("sofia").roles("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("SC-0001", "SCOOTER", centralSquare.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Requirement("FM-21")
    void forbidsEndUserFromRegisteringVehicle() throws Exception {
        mockMvc.perform(post(VEHICLES_URL)
                        .with(user("ines").roles("END_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehicleJson("EB-0001", "E_BIKE", centralSquare.getId())))
                .andExpect(status().isForbidden());
    }

    private static String vehicleJson(String code, String type, long stationId) {
        return """
                {
                  "code": "%s",
                  "type": "%s",
                  "stationId": %d
                }
                """.formatted(code, type, stationId);
    }
}