package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.services.PaymentMethodService;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.CardDeclinedException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.PaymentGatewayException;

@WebMvcTest(PaymentMethodController.class)
@Import(SecurityConfig.class)
class PaymentMethodControllerTest {

    private static final String PAYMENT_METHODS_URL = "/api/payment-methods";

    private final MockMvc mockMvc;

    @MockitoBean
    private PaymentMethodService paymentMethodService;

    @Autowired
    PaymentMethodControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-32")
    void linksTheCardAndShowsOnlyItsLastFourDigits() throws Exception {
        when(paymentMethodService.link("ines", "pm_card_visa"))
                .thenReturn(savedCard(1L, "pm_gateway_secret", "4242", true));

        link("{\"cardToken\": \"pm_card_visa\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.last4").value("4242"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andExpect(jsonPath("$.gatewayToken").doesNotExist());
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-32")
    void aCardDeclinedByTheSandboxIsRefusedWithPaymentRequired() throws Exception {
        when(paymentMethodService.link("ines", "pm_card_chargeDeclined"))
                .thenThrow(new CardDeclinedException());

        link("{\"cardToken\": \"pm_card_chargeDeclined\"}")
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-32")
    void anUnavailableGatewayIsABadGatewayNotADeclinedCard() throws Exception {
        when(paymentMethodService.link("ines", "pm_card_visa"))
                .thenThrow(new PaymentGatewayException("Stripe answered 500"));

        link("{\"cardToken\": \"pm_card_visa\"}")
                .andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-32")
    void aBlankCardTokenIsABadRequest() throws Exception {
        link("{\"cardToken\": \" \"}").andExpect(status().isBadRequest());

        verifyNoInteractions(paymentMethodService);
    }

    @Test
    @WithMockUser(username = "ines", roles = "END_USER")
    @Requirement("FM-32")
    void listsTheCardsOfTheAuthenticatedUser() throws Exception {
        when(paymentMethodService.cardsOf("ines"))
                .thenReturn(List.of(savedCard(1L, "pm_gateway_1", "4242", true)));

        mockMvc.perform(get(PAYMENT_METHODS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].last4").value("4242"))
                .andExpect(jsonPath("$[0].gatewayToken").doesNotExist());
    }

    @Test
    @WithMockUser(username = "sofia", roles = "OPERATOR")
    @Requirement("FM-32")
    void anotherRoleCannotLinkACard() throws Exception {
        link("{\"cardToken\": \"pm_card_visa\"}").andExpect(status().isForbidden());

        verifyNoInteractions(paymentMethodService);
    }

    @Test
    @Requirement("FM-32")
    void linkingWithoutAuthenticationIsRejected() throws Exception {
        link("{\"cardToken\": \"pm_card_visa\"}").andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentMethodService);
    }

    private ResultActions link(String body) throws Exception {
        return mockMvc.perform(post(PAYMENT_METHODS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static PaymentMethod savedCard(Long id, String token, String last4, boolean isDefault) {
        PaymentMethod card = new PaymentMethod("ines", token, last4);
        if (isDefault) {
            card.makeDefault();
        }
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }
}
