package pt.ua.tqs.flexmotion_cycles.services;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pt.ua.tqs.flexmotion_cycles.config.PaymentProperties;
import pt.ua.tqs.flexmotion_cycles.data.PaymentMethodRepository;
import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.PaymentMethodRequiredException;

/** Payment conditions of a trip: a linked card (BR2) and the deposit held on it (BR16). */
@Service
public class DepositService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentPort paymentPort;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    public DepositService(PaymentMethodRepository paymentMethodRepository,
            PaymentRepository paymentRepository, PaymentPort paymentPort,
            PaymentProperties paymentProperties, Clock clock) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
        this.paymentPort = paymentPort;
        this.paymentProperties = paymentProperties;
        this.clock = clock;
    }

    /** BR2: without a linked payment method a trip is refused. */
    public PaymentMethod requireDefaultCard(String username) {
        return paymentMethodRepository.findByUsernameAndDefaultMethodTrue(username)
                .orElseThrow(PaymentMethodRequiredException::new);
    }

    /**
     * BR16: the deposit is authorized on the card; a declined authorization refuses the trip.
     * If the surrounding transaction later rolls back (e.g. the unlock command fails or a
     * concurrent unlock wins), the authorization is released so no money stays reserved.
     */
    public Payment authorizeDeposit(Trip trip, PaymentMethod card) {
        Payment deposit = new Payment(PaymentType.DEPOSIT, paymentProperties.depositAmount(),
                card, trip, clock.instant());
        PaymentResult result = paymentPort.authorize(card.getGatewayToken(), deposit.getAmount());
        if (!result.approved()) {
            throw new UnlockRefusedException("The deposit authorization was declined");
        }
        deposit.markAuthorized(result.gatewayRef());
        releaseIfRolledBack(result.gatewayRef());
        return paymentRepository.save(deposit);
    }

    private void releaseIfRolledBack(String gatewayRef) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    paymentPort.release(gatewayRef);
                }
            }
        });
    }
}
