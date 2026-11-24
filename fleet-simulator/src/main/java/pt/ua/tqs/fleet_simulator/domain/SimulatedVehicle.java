package pt.ua.tqs.fleet_simulator.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The simulated hardware of one vehicle: battery, position, lock and the next fault to report.
 * It applies no business rules: the platform decides what the readings mean.
 */
public class SimulatedVehicle {

    static final int MIN_BATTERY_PERCENT = 0;
    static final int MAX_BATTERY_PERCENT = 100;

    /** The code is a level of the MQTT topic, so it cannot hold "/", "+" or "#". */
    private static final Pattern VALID_CODE = Pattern.compile("[^/+#\\s]+");

    private final String code;
    private int batteryPercent;
    private GeoPoint position;
    private boolean locked = true;
    private FaultEvent pendingFault;

    public SimulatedVehicle(String code, int batteryPercent, GeoPoint position) {
        if (code == null || !VALID_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid vehicle code: '" + code + "'");
        }
        if (batteryPercent < MIN_BATTERY_PERCENT || batteryPercent > MAX_BATTERY_PERCENT) {
            throw new IllegalArgumentException("Battery of " + code + " must be between "
                    + MIN_BATTERY_PERCENT + " and " + MAX_BATTERY_PERCENT + " %");
        }
        this.code = code;
        this.batteryPercent = batteryPercent;
        this.position = Objects.requireNonNull(position, "position");
    }

    public String code() {
        return code;
    }

    public void unlock() {
        locked = false;
    }

    /** Closes the lock where the vehicle is. */
    public void lock() {
        locked = true;
    }

    /** Closes the lock at the given position, as a user does when docking the vehicle. */
    public void lockAt(GeoPoint dockPosition) {
        position = Objects.requireNonNull(dockPosition, "dockPosition");
        locked = true;
    }

    /** The battery drains only while the vehicle is in use (lock open), never below 0 %. */
    public void drain(int percent) {
        if (!locked) {
            batteryPercent = Math.max(MIN_BATTERY_PERCENT, batteryPercent - percent);
        }
    }

    public void triggerFault(FaultEvent fault) {
        pendingFault = Objects.requireNonNull(fault, "fault");
    }

    /** Builds the current reading. A pending fault goes into this reading only. */
    public TelemetryReading takeReading(Instant timestamp) {
        TelemetryReading reading = new TelemetryReading(
                code, timestamp, batteryPercent, position, locked, pendingFault);
        pendingFault = null;
        return reading;
    }
}