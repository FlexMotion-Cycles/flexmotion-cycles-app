package pt.ua.tqs.flexmotion_cycles.domain.exceptions;

import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;

public class InvalidStateTransitionException extends DomainException {

    private final VehicleState from;
    private final VehicleState to;

    public InvalidStateTransitionException(VehicleState from, VehicleState to) {
        super("Invalid vehicle state transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public VehicleState getFrom() {
        return from;
    }

    public VehicleState getTo() {
        return to;
    }
}
