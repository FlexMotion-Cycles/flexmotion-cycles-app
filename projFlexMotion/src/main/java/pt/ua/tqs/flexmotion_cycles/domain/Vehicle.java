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

/**
 * A fleet vehicle of any {@link VehicleType}. Every new vehicle starts as
 * {@link VehicleState#AVAILABLE}, the initial state of the lifecycle.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleState state;

    /** Station where the vehicle is docked; null while it is away from a station. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    /**
     * Optimistic locking: concurrent state changes on the same vehicle fail instead of
     * overwriting each other.
     */
    @Version
    private Long version;

    /** Required by JPA. */
    protected Vehicle() {
    }

    /** Creates a new vehicle docked at the given station, in the AVAILABLE state. */
    public Vehicle(String code, VehicleType type, Station station) {
        this.code = code;
        this.type = type;
        this.station = station;
        this.state = VehicleState.AVAILABLE;
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

    public VehicleState getState() {
        return state;
    }

    public Station getStation() {
        return station;
    }
}