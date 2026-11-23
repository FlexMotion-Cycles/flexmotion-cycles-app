package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.services.TripService;
import pt.ua.tqs.flexmotion_cycles.services.UnlockRefusedException;
import pt.ua.tqs.flexmotion_cycles.services.VehicleNotFoundException;

@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
class TripControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    @Autowired
    TripControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(username = "ines")
    @Requirement("FM-27")
    void unlockRequestsATripForTheAuthenticatedUser() throws Exception {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        Vehicle ebike = new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
        when(tripService.requestUnlock("ines", "EB-01"))
                .thenReturn(new Trip("ines", ebike, Instant.parse("2026-11-22T10:15:00Z")));

        unlock("EB-01")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.vehicleCode").value("EB-01"))
                .andExpect(jsonPath("$.status").value("UNLOCK_REQUESTED"));
        verify(tripService).requestUnlock("ines", "EB-01");
    }

    @Test
    @Requirement("FM-27")
    void unlockWithoutAuthenticationIsRejected() throws Exception {
        unlock("EB-01").andExpect(status().isUnauthorized());

        verifyNoInteractions(tripService);
    }

    @Test
    @WithMockUser(username = "ines")
    @Requirement("FM-27")
    void unlockOfUnknownVehicleReturnsNotFound() throws Exception {
        when(tripService.requestUnlock("ines", "XX-99"))
                .thenThrow(new VehicleNotFoundException("XX-99"));

        unlock("XX-99").andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ines")
    @Requirement("FM-27")
    void refusedUnlockReturnsConflict() throws Exception {
        when(tripService.requestUnlock("ines", "EB-04"))
                .thenThrow(new UnlockRefusedException("Vehicle EB-04 cannot be unlocked"));

        unlock("EB-04")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vehicle EB-04 cannot be unlocked"));
    }

    @Test
    @WithMockUser(username = "ines")
    @Requirement("FM-27")
    void concurrentUnlockReturnsConflict() throws Exception {
        when(tripService.requestUnlock("ines", "EB-01"))
                .thenThrow(new ObjectOptimisticLockingFailureException(Vehicle.class, 1L));

        unlock("EB-01").andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "ines")
    @Requirement("FM-27")
    void blankVehicleCodeIsRejected() throws Exception {
        unlock(" ").andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    private ResultActions unlock(String vehicleCode) throws Exception {
        return mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleCode\": \"" + vehicleCode + "\"}"));
    }
}
