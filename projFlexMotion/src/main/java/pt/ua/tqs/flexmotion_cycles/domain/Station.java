package pt.ua.tqs.flexmotion_cycles.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A place where vehicles are docked. Conventional stations have an overflow zone (BR11);
 * accessible hubs have step-free access information and technician hours.
 */
@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private GeoPoint location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StationType type;

    @Column(nullable = false)
    private int dockCapacity;

    /** Conventional stations only; null for accessible hubs. */
    private Double overflowRadiusMeters;

    /** Accessible hubs only; null for conventional stations. */
    private Boolean stepFreeAccess;

    /** Accessible hubs only; null for conventional stations. */
    private LocalTime technicianFrom;

    /** Accessible hubs only; null for conventional stations. */
    private LocalTime technicianTo;

    /** Required by JPA. */
    protected Station() {
    }

    private Station(String name, GeoPoint location, StationType type, int dockCapacity) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.dockCapacity = dockCapacity;
    }

    /** Creates a conventional station with an overflow zone of the given radius. */
    public static Station conventional(String name, GeoPoint location, int dockCapacity,
            double overflowRadiusMeters) {
        Station station = new Station(name, location, StationType.CONVENTIONAL, dockCapacity);
        station.overflowRadiusMeters = overflowRadiusMeters;
        return station;
    }

    /** Creates an accessible hub with its step-free access flag and technician hours. */
    public static Station accessible(String name, GeoPoint location, int dockCapacity,
            boolean stepFreeAccess, LocalTime technicianFrom, LocalTime technicianTo) {
        Station station = new Station(name, location, StationType.ACCESSIBLE, dockCapacity);
        station.stepFreeAccess = stepFreeAccess;
        station.technicianFrom = technicianFrom;
        station.technicianTo = technicianTo;
        return station;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public StationType getType() {
        return type;
    }

    public int getDockCapacity() {
        return dockCapacity;
    }

    public Double getOverflowRadiusMeters() {
        return overflowRadiusMeters;
    }

    public Boolean getStepFreeAccess() {
        return stepFreeAccess;
    }

    public LocalTime getTechnicianFrom() {
        return technicianFrom;
    }

    public LocalTime getTechnicianTo() {
        return technicianTo;
    }
}