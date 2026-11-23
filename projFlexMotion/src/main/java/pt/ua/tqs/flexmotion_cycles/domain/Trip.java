package pt.ua.tqs.flexmotion_cycles.domain;

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
import java.time.Instant;

/** Prototype: only what FM-27 needs to request an unlock and start a trip. */
@Entity
@Table(name = "trips")
public class Trip {

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
}
