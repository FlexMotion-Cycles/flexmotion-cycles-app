package pt.ua.tqs.flexmotion_cycles.domain;

import java.math.BigDecimal;
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

/** Prototype: only what FM-27 needs to request an unlock and start a trip. */
@Entity
@Table(name = "trips")
public class Trip {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_station_id", nullable = false)
    private Station startStation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    /** Time the lock opened (BR7); null until then. */
    private Instant startedAt;

    private Instant endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_station_id")
    private Station endStation;

    // Cost breakdown (BR6, BR7, BR17), stored per column; all null until the trip ends.
    @Column(precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal unlockFee;

    private Long billedMinutes;

    private Long overtimeMinutes;

    @Column(precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal usageCost;

    @Column(precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal overtimeCost;

    @Column(precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal reservationFee;

    @Column(precision = MONEY_PRECISION, scale = MONEY_SCALE)
    private BigDecimal totalCost;

    protected Trip() {
        // Required by JPA.
    }

    /** Unlock requested at the vehicle's current station; the trip has not started yet. */
    public Trip(String username, Vehicle vehicle, Instant requestedAt) {
        this.username = username;
        this.vehicle = vehicle;
        this.startStation = vehicle.getStation();
        this.requestedAt = requestedAt;
        this.status = TripStatus.UNLOCK_REQUESTED;
    }

    /** The lock opened: trip time counts from here (BR7). */
    public void start(Instant startedAt) {
        if (status != TripStatus.UNLOCK_REQUESTED) {
            throw new IllegalStateException("Trip " + id + " is " + status + ", not UNLOCK_REQUESTED");
        }
        this.status = TripStatus.ACTIVE;
        this.startedAt = startedAt;
    }

    public void end(Instant endedAt, Station endStation, TripCost cost) {
        Objects.requireNonNull(endedAt, "endedAt");
        Objects.requireNonNull(endStation, "endStation");
        Objects.requireNonNull(cost, "cost");
        if (status != TripStatus.ACTIVE) {
            throw new IllegalStateException("Trip " + id + " is " + status + ", not ACTIVE");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Trip " + id + " cannot end before it started");
        }
        this.status = TripStatus.ENDED;
        this.endedAt = endedAt;
        this.endStation = endStation;
        this.unlockFee = cost.unlockFee();
        this.billedMinutes = cost.billedMinutes();
        this.overtimeMinutes = cost.overtimeMinutes();
        this.usageCost = cost.usageCost();
        this.overtimeCost = cost.overtimeCost();
        this.reservationFee = cost.reservationFee();
        this.totalCost = cost.total();
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

    public Station getStartStation() {
        return startStation;
    }

    public TripStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }


    public Instant getEndedAt() {
        return endedAt;
    }

    public Station getEndStation() {
        return endStation;
    }

    public TripCost getCost() {
        if (status != TripStatus.ENDED) {
            return null;
        }
        return new TripCost(unlockFee, billedMinutes, overtimeMinutes, usageCost, overtimeCost, reservationFee, totalCost);
    }
}
