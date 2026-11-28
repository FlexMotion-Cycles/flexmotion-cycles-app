package pt.ua.tqs.flexmotion_cycles.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.ports.CardLinkResult;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.CardDeclinedException;

/** Links cards through the payment sandbox (US-14, BR2). */
@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentPort paymentPort;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository,
            PaymentPort paymentPort) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentPort = paymentPort;
    }

    /**
     * Links the card only if the sandbox approves it; the user's first card becomes the default.
     * The gateway is called outside a transaction so no database connection waits on the network.
     */
    public PaymentMethod link(String username, String cardToken) {
        CardLinkResult result = paymentPort.linkCard(cardToken);
        if (!result.approved()) {
            throw new CardDeclinedException();
        }
        PaymentMethod card = new PaymentMethod(username, result.gatewayToken(), result.last4());
        if (!paymentMethodRepository.existsByUsername(username)) {
            card.makeDefault();
        }
        return paymentMethodRepository.save(card);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> cardsOf(String username) {
        return paymentMethodRepository.findByUsernameOrderByIdAsc(username);
    }
}
