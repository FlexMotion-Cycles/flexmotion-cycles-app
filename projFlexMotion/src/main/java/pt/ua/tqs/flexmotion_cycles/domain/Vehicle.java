package pt.ua.tqs.flexmotion_cycles.domain;

import java.util.Objects;

import java.time.Instant;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

/**
 * A fleet vehicle of any {@link VehicleType}. Every new vehicle starts as
 * {@link VehicleStatus#AVAILABLE}, the initial state of the lifecycle.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifier shared with the telemetry feed (e.g. "EB-01"). */
    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    /** Last reported battery level; 0 until the first telemetry reading. */
    @Column(nullable = false)
    private int batteryLevel;

    /** Station where the vehicle is docked; null while it is away from a station. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false)
    private boolean locked = true;

    @Embedded
    @AttributeOverride(name = "latitude", column = @Column(name = "position_latitude"))
    @AttributeOverride(name = "longitude", column = @Column(name = "position_longitude"))
    private GeoPoint position;

    private Instant lastTelemetryAt;

    /** Optimistic locking: concurrent state changes on the same vehicle fail. */
    @Version
    private Long version;

    protected Vehicle() {
        // Required by JPA.
    }

    /** A new vehicle starts as AVAILABLE at its station. */
    public Vehicle(String code, VehicleType type, int batteryLevel, Station station) {
        this.code = code;
        this.type = type;
        this.batteryLevel = batteryLevel;
        this.station = station;
        this.status = VehicleStatus.AVAILABLE;
    }

    /** Registers a new vehicle docked at the given station; its battery is not known yet. */
    public Vehicle(String code, VehicleType type, Station station) {
        this(code, type, 0, station);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public VehicleType getType() {
        return type;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    /** Same lifecycle state as {@link #getStatus()}; kept until both enums are unified. */
    public VehicleState getState() {
        return VehicleState.valueOf(status.name());
    }

    /** Only {@link VehicleLifecycle} should call this, after checking the transition. */
    public void setState(VehicleState state) {
        this.status = VehicleStatus.valueOf(state.name());
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public Station getStation() {
        return station;
    }

    public Long getVersion() {
        return version;
    }

    /** AVAILABLE → IN_USE: the lock opened and the vehicle left its dock. */
    public void startUse() {
        if (status != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle " + code + " is " + status + ", not AVAILABLE");
        }
        status = VehicleStatus.IN_USE;
        station = null;
    }

    /** IN_USE → AVAILABLE: the lock closed at a dock of the given station (BR9). */
    public void endUse(Station station) {
        Objects.requireNonNull(station, "station");
        if (status != VehicleStatus.IN_USE) {
            throw new IllegalStateException("Vehicle " + code + " is " + status + ", not IN_USE");
        }
        status = VehicleStatus.AVAILABLE;
        this.station = station;
    }
    public boolean isLocked() {
        return locked;
    }

    public GeoPoint getPosition() {
        return position;
    }

    public Instant getLastTelemetryAt() {
        return lastTelemetryAt;
    }

    public boolean applyTelemetry(TelemetryReading reading) {
        if (lastTelemetryAt != null && !reading.timestamp().isAfter(lastTelemetryAt)) {
            return false;
        }
        batteryLevel = reading.batteryPercent();
        position = reading.position();
        locked = reading.locked();
        lastTelemetryAt = reading.timestamp();
        return true;
    }

    public void dockAt(Station station) {
        this.station = station;
    }

    public void undock() {
        this.station = null;
    }
}
