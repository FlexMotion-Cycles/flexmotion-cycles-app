package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleControllerIT {

    private static final String VEHICLES_URL = "/api/vehicles";
    private static final GeoPoint VALE_CLARO_CENTRE = new GeoPoint(40.6405, -8.6538);
    private static final Long UNKNOWN_ID = 999L;

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


    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Operator deactivates an AVAILABLE vehicle: 200 and MAINTENANCE")
    void operatorDeactivatesAvailableVehicle() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/deactivate", vehicle.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicle.getId()))
                .andExpect(jsonPath("$.state").value("MAINTENANCE"));

        assertEquals(VehicleState.MAINTENANCE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Operator activates a vehicle in MAINTENANCE: 200 and AVAILABLE")
    void operatorActivatesVehicleInMaintenance() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.MAINTENANCE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/activate", vehicle.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AVAILABLE"));

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Deactivating a vehicle IN_USE returns 409 and the state does not change")
    void deactivateVehicleInUseReturnsConflict() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.IN_USE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isConflict());

        assertEquals(VehicleState.IN_USE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Activating an AVAILABLE vehicle returns 409 and the state does not change")
    void activateAvailableVehicleReturnConflict() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/activate", vehicle.getId()).with(csrf())).andExpect(status().isConflict());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Deactivating an unknown vehicle returns 404")
    void deactivateUnknownVehicleReturnsNotFound() throws Exception {
        mockMvc.perform(patch(VEHICLES_URL + "/{id}/deactivate", UNKNOWN_ID).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Activating an unknown vehicle returns 404")
    void activateUnknownVehicleReturnsNotFound() throws Exception {
        mockMvc.perform(patch(VEHICLES_URL + "/{id}/activate", UNKNOWN_ID).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "END_USER")
    @DisplayName("A user without the OPERATOR role gets 403 and the state does not change")
    void nonOperatorIsForbidden() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);
        mockMvc.perform(patch(VEHICLES_URL + "/{id}/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isForbidden());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "END_USER")
    @DisplayName("A user without the OPERATOR role cannot activate a vehicle: 403")
    void nonOperatorCannotActivate() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.MAINTENANCE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/activate", vehicle.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertEquals(VehicleState.MAINTENANCE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("An unauthenticated request gets 401")
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockMvc.perform(patch(VEHICLES_URL + "/{id}/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isUnauthorized());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    private Vehicle savedVehicleIn(VehicleState state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, null);
        vehicle.setState(state);
        return vehicleRepository.save(vehicle);
    }

    private VehicleState stateInDatabase(Long id) {
        return vehicleRepository.findById(id).orElseThrow().getState();
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