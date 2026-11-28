package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.CardDeclinedException;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    private static final String INES = "ines";
    private static final String CARD_TOKEN = "pm_card_visa";

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentPort paymentPort;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    @Test
    @Requirement("FM-32")
    void linksAnApprovedCardKeepingOnlyTheGatewayTokenAndLastFourDigits() {
        when(paymentPort.linkCard(CARD_TOKEN))
                .thenReturn(CardLinkResult.approved("pm_gateway_1", "4242"));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).then(returnsFirstArg());

        PaymentMethod card = paymentMethodService.link(INES, CARD_TOKEN);

        assertThat(card.getUsername()).isEqualTo(INES);
        assertThat(card.getGatewayToken()).isEqualTo("pm_gateway_1");
        assertThat(card.getLast4()).isEqualTo("4242");
        verify(paymentMethodRepository).save(card);
    }

    @Test
    @Requirement("FM-32")
    void theFirstLinkedCardBecomesTheDefault() {
        when(paymentPort.linkCard(CARD_TOKEN))
                .thenReturn(CardLinkResult.approved("pm_gateway_1", "4242"));
        when(paymentMethodRepository.existsByUsername(INES)).thenReturn(false);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).then(returnsFirstArg());

        PaymentMethod card = paymentMethodService.link(INES, CARD_TOKEN);

        assertThat(card.isDefault()).isTrue();
    }

    @Test
    @Requirement("FM-32")
    void aFurtherCardDoesNotReplaceTheDefault() {
        when(paymentPort.linkCard(CARD_TOKEN))
                .thenReturn(CardLinkResult.approved("pm_gateway_2", "4242"));
        when(paymentMethodRepository.existsByUsername(INES)).thenReturn(true);
        when(paymentMethodRepository.save(any(PaymentMethod.class))).then(returnsFirstArg());

        PaymentMethod card = paymentMethodService.link(INES, CARD_TOKEN);

        assertThat(card.isDefault()).isFalse();
    }

    @Test
    @Requirement("FM-32")
    void aCardDeclinedByTheSandboxIsNotLinked() {
        when(paymentPort.linkCard("pm_card_chargeDeclined")).thenReturn(CardLinkResult.declined());

        assertThatThrownBy(() -> paymentMethodService.link(INES, "pm_card_chargeDeclined"))
                .isInstanceOf(CardDeclinedException.class);
        verify(paymentMethodRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-32")
    void listsTheCardsOfTheUser() {
        PaymentMethod card = new PaymentMethod(INES, "pm_gateway_1", "4242");
        when(paymentMethodRepository.findByUsernameOrderByIdAsc(INES)).thenReturn(List.of(card));

        assertThat(paymentMethodService.cardsOf(INES)).containsExactly(card);
    }
}
