package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PaymentMethodRepositoryIT {

    private final PaymentMethodRepository paymentMethodRepository;
    private final TestEntityManager entityManager;

    @Autowired
    PaymentMethodRepositoryIT(PaymentMethodRepository paymentMethodRepository,
            TestEntityManager entityManager) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.entityManager = entityManager;
    }

    private PaymentMethod persistCard(String username, String token, boolean isDefault) {
        PaymentMethod card = new PaymentMethod(username, token, "4242");
        if (isDefault) {
            card.makeDefault();
        }
        return entityManager.persistFlushFind(card);
    }

    @Test
    @Requirement("FM-32")
    void keepsTheTokenLastFourDigitsAndDefaultFlag() {
        Long id = persistCard("ines", "pm_gateway_1", true).getId();
        entityManager.clear();

        PaymentMethod found = paymentMethodRepository.findById(id).orElseThrow();

        assertThat(found.getGatewayToken()).isEqualTo("pm_gateway_1");
        assertThat(found.getLast4()).isEqualTo("4242");
        assertThat(found.isDefault()).isTrue();
    }

    @Test
    @Requirement("FM-32")
    void listsOnlyTheCardsOfTheUserInLinkingOrder() {
        PaymentMethod first = persistCard("ines", "pm_gateway_1", true);
        PaymentMethod second = persistCard("ines", "pm_gateway_2", false);
        persistCard("rui", "pm_gateway_3", true);

        assertThat(paymentMethodRepository.findByUsernameOrderByIdAsc("ines"))
                .extracting(PaymentMethod::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    @Requirement("FM-32")
    void findsTheDefaultCardOfTheUser() {
        persistCard("ines", "pm_gateway_1", false);
        PaymentMethod defaultCard = persistCard("ines", "pm_gateway_2", true);
        persistCard("rui", "pm_gateway_3", true);

        assertThat(paymentMethodRepository.findByUsernameAndDefaultMethodTrue("ines"))
                .map(PaymentMethod::getId)
                .contains(defaultCard.getId());
    }

    @Test
    @Requirement("FM-32")
    void aUserWithoutCardsHasNoDefaultCard() {
        persistCard("rui", "pm_gateway_3", true);

        assertThat(paymentMethodRepository.existsByUsername("ines")).isFalse();
        assertThat(paymentMethodRepository.findByUsernameAndDefaultMethodTrue("ines")).isEmpty();
    }
}
