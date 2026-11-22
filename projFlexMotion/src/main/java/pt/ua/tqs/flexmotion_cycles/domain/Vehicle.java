package pt.ua.tqs.flexmotion_cycles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
    private VehicleState state = VehicleState.AVAILABLE;

    @Column(nullable = false)
    private int batteryPercent;

    @Column(nullable = false)
    private boolean locked;

    @Version
    private Long version;

    protected Vehicle() {}

    public Vehicle(String code, VehicleType type, int batteryPercent, boolean locked) {
        this.code = code;
        this.type = type;
        this.batteryPercent = batteryPercent;
        this.locked = locked;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public VehicleState getState() {
        return state;
    }

    public void setState(VehicleState state) {
        this.state = state;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Long getVersion() {
        return version;
    }
}
