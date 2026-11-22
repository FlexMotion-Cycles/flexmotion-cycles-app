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
import jakarta.persistence.Version;

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

    @Column(nullable = false)
    private int batteryLevel;

    /** Current station; null while the vehicle is in use. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    /** Optimistic locking: concurrent state changes on the same vehicle fail. */
    @Version
    private long version;

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

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public Station getStation() {
        return station;
    }

    public long getVersion() {
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
}
