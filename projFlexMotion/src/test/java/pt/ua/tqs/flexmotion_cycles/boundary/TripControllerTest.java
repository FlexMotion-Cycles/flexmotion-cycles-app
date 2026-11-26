package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ua.tqs.flexmotion_cycles.domain.TripCost;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripCost;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.services.TripService;
import pt.ua.tqs.flexmotion_cycles.services.UnlockRefusedException;
import pt.ua.tqs.flexmotion_cycles.services.VehicleNotFoundException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.TripNotFoundException;

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
    @WithMockUser(username = "ines", roles = "END_USER")
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
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-27")
    void unlockOfUnknownVehicleReturnsNotFound() throws Exception {
        when(tripService.requestUnlock("ines", "XX-99"))
                .thenThrow(new VehicleNotFoundException("XX-99"));

        unlock("XX-99").andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-27")
    void refusedUnlockReturnsConflict() throws Exception {
        when(tripService.requestUnlock("ines", "EB-04"))
                .thenThrow(new UnlockRefusedException("Vehicle EB-04 cannot be unlocked"));

        unlock("EB-04")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Vehicle EB-04 cannot be unlocked"));
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-27")
    void concurrentUnlockReturnsConflict() throws Exception {
        when(tripService.requestUnlock("ines", "EB-01"))
                .thenThrow(new ObjectOptimisticLockingFailureException(Vehicle.class, 1L));

        unlock("EB-01").andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-27")
    void blankVehicleCodeIsRejected() throws Exception {
        unlock(" ").andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-28")
    void endedTripShowsWhereItEndedAndItsCostBreakdown() throws Exception {
        when(tripService.findTrip("ines", 7L)).thenReturn(endedTrip());

        mockMvc.perform(get("/api/trips/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.endedAt").value("2026-11-22T10:01:01Z"))
                .andExpect(jsonPath("$.endStationName").value("Central"))
                .andExpect(jsonPath("$.cost.billedMinutes").value(2))
                .andExpect(jsonPath("$.cost.overtimeMinutes").value(0))
                .andExpect(content().string(containsString("\"unlockFee\":1.00")))
                .andExpect(content().string(containsString("\"usageCost\":0.30")))
                .andExpect(content().string(containsString("\"total\":1.30")));
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-28")
    void activeTripHasNoCostYet() throws Exception {
        Trip trip = new Trip("ines", centralEbike(), Instant.parse("2026-11-22T09:59:50Z"));
        trip.start(Instant.parse("2026-11-22T10:00:00Z"));
        when(tripService.findTrip("ines", 7L)).thenReturn(trip);

        mockMvc.perform(get("/api/trips/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endedAt").value(nullValue()))
                .andExpect(jsonPath("$.cost").value(nullValue()));
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-28")
    void tripOfAnotherUserOrUnknownReturnsNotFound() throws Exception {
        when(tripService.findTrip("ines", 8L)).thenThrow(new TripNotFoundException(8L));

        mockMvc.perform(get("/api/trips/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Trip 8 not found"));
    }

    @Test
    @Requirement("FM-28")
    void getTripWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/trips/7")).andExpect(status().isUnauthorized());

        verifyNoInteractions(tripService);
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-28")
    void nonNumericTripIdIsRejected() throws Exception {
        mockMvc.perform(get("/api/trips/abc")).andExpect(status().isBadRequest());

        verifyNoInteractions(tripService);
    }

    private static Vehicle centralEbike() {
        Station central = new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL);
        return new Vehicle("EB-01", VehicleType.E_BIKE, 80, central);
    }

    /** Trip of 61 s ended at Central: 2 billed minutes, 1.30 € (BR7). */
    private static Trip endedTrip() {
        Vehicle ebike = centralEbike();
        Station central = ebike.getStation();
        Trip trip = new Trip("ines", ebike, Instant.parse("2026-11-22T09:59:50Z"));
        ReflectionTestUtils.setField(trip, "id", 7L);
        ebike.startUse();
        trip.start(Instant.parse("2026-11-22T10:00:00Z"));
        trip.end(Instant.parse("2026-11-22T10:01:01Z"), central,
                new TripCost(new BigDecimal("1.00"), 2, 0, new BigDecimal("0.30"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("1.30")));
        return trip;
    }

    private ResultActions unlock(String vehicleCode) throws Exception {
        return mockMvc.perform(post("/api/trips")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleCode\": \"" + vehicleCode + "\"}"));
    }
}
