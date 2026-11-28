package pt.ua.tqs.flexmotion_cycles.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A money movement on a linked card. Its status follows the gateway (BR8, BR16); invalid
 * transitions are rejected.
 */
@Entity
@Table(name = "payments")
public class Payment {

    private static final int MONEY_PRECISION = 10;
    private static final int MONEY_SCALE = 2;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = Map.of(
            PaymentStatus.PENDING,
            EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.DECLINED),
            PaymentStatus.AUTHORIZED,
            EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.RELEASED),
            PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.REFUNDED),
            PaymentStatus.DECLINED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.RELEASED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false, precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** Gateway id of the operation; null while PENDING, needed later to capture or release. */
    private String gatewayRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    /** Trip this payment is for, if any; booking links come with US-16. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    /** Reservation whose fee this payment charges (BR6), if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private Instant createdAt;

    protected Payment() {
        // Required by JPA.
    }

    /** A payment about to be sent to the gateway; it starts PENDING. */
    public Payment(PaymentType type, BigDecimal amount, PaymentMethod paymentMethod, Trip trip,
            Instant createdAt) {
        this(type, amount, paymentMethod, createdAt);
        this.trip = trip;
    }

    /** The fee of a reservation, about to be sent to the gateway; it starts PENDING. */
    public Payment(PaymentType type, BigDecimal amount, PaymentMethod paymentMethod,
            Reservation reservation, Instant createdAt) {
        this(type, amount, paymentMethod, createdAt);
        this.reservation = reservation;
    }

    private Payment(PaymentType type, BigDecimal amount, PaymentMethod paymentMethod,
            Instant createdAt) {
        this.type = Objects.requireNonNull(type, "type");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A payment amount must be positive");
        }
        this.status = PaymentStatus.PENDING;
    }

    /** The gateway reserved the amount without charging it (e.g. the deposit, BR16). */
    public void markAuthorized(String gatewayRef) {
        String reference = requireText(gatewayRef);
        moveTo(PaymentStatus.AUTHORIZED);
        this.gatewayRef = reference;
    }

    /** The gateway charged the amount, directly or by capturing an authorization. */
    public void markCaptured(String gatewayRef) {
        String reference = requireText(gatewayRef);
        moveTo(PaymentStatus.CAPTURED);
        this.gatewayRef = reference;
    }

    public void markDeclined() {
        moveTo(PaymentStatus.DECLINED);
    }

    public void markReleased() {
        moveTo(PaymentStatus.RELEASED);
    }

    public void markRefunded() {
        moveTo(PaymentStatus.REFUNDED);
    }

    private void moveTo(PaymentStatus next) {
        if (!ALLOWED.get(status).contains(next)) {
            throw new IllegalStateException(
                    "Payment " + id + " cannot go from " + status + " to " + next);
        }
        this.status = next;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("gatewayRef must not be blank");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public PaymentType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getGatewayRef() {
        return gatewayRef;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Trip getTrip() {
        return trip;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
