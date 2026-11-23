package pt.ua.tqs.flexmotion_cycles.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc 
@Import(TestcontainersConfiguration.class)
@DisplayName("Vehicle REST API (US-04)")
public class VehicleControllerIT {
    private static final String BASE_URL = "/api/vehicles/{id}";
    private static final Long UNKNOWN_ID = 999L;

    private final MockMvc mockmvc;
    private final VehicleRepository vehicleRepository;

    @Autowired 
    public VehicleControllerIT(MockMvc mockmvc, VehicleRepository vehicleRepository) {
        this.mockmvc = mockmvc;
        this.vehicleRepository = vehicleRepository;
    }

    @BeforeEach 
    void setUp() {
        vehicleRepository.deleteAll();
    }

    private Vehicle savedVehicleIn(VehicleState state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, 80, true);
        vehicle.setState(state);
        return vehicleRepository.save(vehicle);
    }

    private VehicleState stateInDatabase(Long id) {
        return vehicleRepository.findById(id).orElseThrow().getState();
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Operator deactivates an AVAILABLE vehicle: 200 and MAINTENANCE")
    void operatorDeactivatesAvailableVehicle() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockmvc.perform(patch(BASE_URL + "/deactivate", vehicle.getId()).with(csrf()))
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

        mockmvc.perform(patch(BASE_URL + "/activate", vehicle.getId()).with(csrf()))
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

        mockmvc.perform(patch(BASE_URL + "/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isConflict());

        assertEquals(VehicleState.IN_USE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Activating an AVAILABLE vehicle returns 409 and the state does not change")
    void activateAvailableVehicleReturnConflict() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockmvc.perform(patch(BASE_URL + "/activate", vehicle.getId()).with(csrf())).andExpect(status().isConflict());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Deactivating an unknown vehicle returns 404")
    void deactivateUnknownVehicleReturnsNotFound() throws Exception {
        mockmvc.perform(patch(BASE_URL + "/deactivate", UNKNOWN_ID).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Activating an unknown vehicle returns 404")
    void activateUnknownVehicleReturnsNotFound() throws Exception {
        mockmvc.perform(patch(BASE_URL + "/activate", UNKNOWN_ID).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "END_USER")
    @DisplayName("A user without the OPERATOR role gets 403 and the state does not change")
    void nonOperatorIsForbidden() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);
        mockmvc.perform(patch(BASE_URL + "/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isForbidden());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @WithMockUser(roles = "END_USER")
    @DisplayName("A user without the OPERATOR role cannot activate a vehicle: 403")
    void nonOperatorCannotActivate() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.MAINTENANCE);

        mockmvc.perform(patch(BASE_URL + "/activate", vehicle.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertEquals(VehicleState.MAINTENANCE, stateInDatabase(vehicle.getId()));
    }

    @Test
    @Requirement("FM-22")
    @DisplayName("An unauthenticated request gets 401")
    void unauthenticatedRequestIsUnauthorized() throws Exception {
        Vehicle vehicle = savedVehicleIn(VehicleState.AVAILABLE);

        mockmvc.perform(patch(BASE_URL + "/deactivate", vehicle.getId()).with(csrf())).andExpect(status().isUnauthorized());

        assertEquals(VehicleState.AVAILABLE, stateInDatabase(vehicle.getId()));
    }


}
