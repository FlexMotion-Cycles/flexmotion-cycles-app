package pt.ua.tqs.flexmotion_cycles.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

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
import jakarta.persistence.Version;

/** Holds one vehicle exclusively for one user until {@code expiresAt} (BR5), for a fixed fee (BR6). */
@Entity
@Table(name = "reservations")
public class Reservation {

    private static final int MONEY_PRECISION = 10;
    private static final int MONEY_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Authenticated username until the User entity exists (US-01). */
    @Column(nullable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    /** Fee at reservation time, so later price changes do not affect it (BR6, BR18). */
    @Column(nullable = false, precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal fee;

    /** Optimistic locking: the expiry job and an unlock cannot both change the reservation. */
    @Version
    private Long version;

    protected Reservation() {
        // Required by JPA.
    }

    /** A new reservation is ACTIVE from {@code createdAt} for the given hold. */
    public Reservation(String username, Vehicle vehicle, Instant createdAt, Duration hold, BigDecimal fee) {
        this.username = Objects.requireNonNull(username, "username");
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = createdAt.plus(Objects.requireNonNull(hold, "hold"));
        this.fee = Objects.requireNonNull(fee, "fee");
        this.status = ReservationStatus.ACTIVE;
    }

    /** BR5: at the exact time limit the reservation has already expired (15:00 = expired). */
    public boolean isDueAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** ACTIVE → FULFILLED: the user unlocked the vehicle in time. */
    public void fulfil(Instant at) {
        requireActive();
        if (isDueAt(at)) {
            throw new IllegalStateException("Reservation " + id + " expired at " + expiresAt);
        }
        status = ReservationStatus.FULFILLED;
    }

    /** ACTIVE → EXPIRED: not unlocked in time (BR5); the fee is still due (BR6). */
    public void expire(Instant at) {
        requireActive();
        if (!isDueAt(at)) {
            throw new IllegalStateException("Reservation " + id + " is held until " + expiresAt);
        }
        status = ReservationStatus.EXPIRED;
    }

    private void requireActive() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation " + id + " is " + status + ", not ACTIVE");
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getFee() {
        return fee;
    }
}
