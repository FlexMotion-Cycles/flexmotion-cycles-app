package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.services.ReservationService;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.ReservationNotFoundException;

/** HTTP rules the acceptance scenarios cannot reach (invalid request, another user's data). */
@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
class ReservationControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Autowired
    ReservationControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-29")
    void blankVehicleCodeIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCode\": \" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservationService);
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-29")
    void reservationOfAnotherUserOrUnknownReturnsNotFound() throws Exception {
        when(reservationService.findReservation("ines", 8L))
                .thenThrow(new ReservationNotFoundException(8L));

        mockMvc.perform(get("/api/reservations/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Reservation 8 not found"));
    }
}
