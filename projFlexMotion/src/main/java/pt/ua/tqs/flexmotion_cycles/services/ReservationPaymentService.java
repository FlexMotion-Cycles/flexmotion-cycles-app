package pt.ua.tqs.flexmotion_cycles.services;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import pt.ua.tqs.flexmotion_cycles.data.PaymentRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;
import pt.ua.tqs.flexmotion_cycles.domain.Reservation;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentPort;
import pt.ua.tqs.flexmotion_cycles.ports.PaymentResult;

/** Payments of a reservation: a linked card to reserve (BR2) and the fee when it expires (BR6). */
@Service
public class ReservationPaymentService {

    private final DepositService depositService;
    private final PaymentRepository paymentRepository;
    private final PaymentPort paymentPort;

    public ReservationPaymentService(DepositService depositService,
            PaymentRepository paymentRepository, PaymentPort paymentPort) {
        this.depositService = depositService;
        this.paymentRepository = paymentRepository;
        this.paymentPort = paymentPort;
    }

    /** BR2: reserving needs a linked card, like unlocking. */
    public void requireCard(String username) {
        depositService.requireDefaultCard(username);
    }

    /**
     * BR6: charges the fee of a reservation that expired unused. A declined charge is kept as
     * DECLINED (still owed); if the expiry rolls back, the charge is refunded so it is never
     * taken twice.
     */
    public Payment chargeFee(Reservation reservation, Instant at) {
        PaymentMethod card = depositService.requireDefaultCard(reservation.getUsername());
        Payment fee = new Payment(PaymentType.RESERVATION_FEE, reservation.getFee(), card,
                reservation, at);
        PaymentResult result = paymentPort.charge(card.getGatewayToken(), fee.getAmount());
        if (result.approved()) {
            fee.markCaptured(result.gatewayRef());
            refundIfRolledBack(result.gatewayRef(), fee.getAmount());
        } else {
            fee.markDeclined();
        }
        return paymentRepository.save(fee);
    }

    private void refundIfRolledBack(String gatewayRef, BigDecimal amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    paymentPort.refund(gatewayRef, amount);
                }
            }
        });
    }
}
