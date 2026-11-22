package pt.ua.tqs.flexmotion_cycles.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pt.ua.tqs.flexmotion_cycles.domain.exceptions.InvalidStateTransitionException;

public class VehicleLifecycle {

    private static final Map<VehicleState, Set<VehicleState>> ALLOWED = new EnumMap<>(VehicleState.class);

    static {
        ALLOWED.put(VehicleState.AVAILABLE, EnumSet.of(VehicleState.RESERVED,VehicleState.IN_USE, VehicleState.MAINTENANCE, VehicleState.REMOVED));
        ALLOWED.put(VehicleState.RESERVED, EnumSet.of(VehicleState.IN_USE, VehicleState.AVAILABLE));
        ALLOWED.put(VehicleState.IN_USE, EnumSet.of(VehicleState.AVAILABLE));
        ALLOWED.put(VehicleState.MAINTENANCE,EnumSet.of(VehicleState.AVAILABLE, VehicleState.REMOVED));
        ALLOWED.put(VehicleState.REMOVED, EnumSet.noneOf(VehicleState.class));
    }

    public boolean isAllowed(VehicleState from, VehicleState to) {
        return ALLOWED.get(from).contains(to);
    }

    public void apply(Vehicle vehicle, VehicleState to) {
        VehicleState from = vehicle.getState();
        if (!isAllowed(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
        vehicle.setState(to);
    }

    public void deactivate(Vehicle vehicle) {
        requireState(vehicle, VehicleState.AVAILABLE, VehicleState.MAINTENANCE);
        vehicle.setState(VehicleState.MAINTENANCE);
    }

    public void activate(Vehicle vehicle) {
        requireState(vehicle, VehicleState.MAINTENANCE, VehicleState.AVAILABLE);
        vehicle.setState(VehicleState.AVAILABLE);
    }

    private static void requireState(Vehicle vehicle, VehicleState required, VehicleState to) {
        if (vehicle.getState() != required) {
            throw new InvalidStateTransitionException(vehicle.getState(), to);
        }
    }
}
