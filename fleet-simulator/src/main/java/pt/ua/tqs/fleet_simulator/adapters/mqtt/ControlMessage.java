package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import pt.ua.tqs.fleet_simulator.domain.FaultEvent;

/** A control message from tests and demos: something that happens to the physical vehicle. */
record ControlMessage(Action action, FaultEvent faultEvent, PositionMessage position) {

    enum Action {
        TRIGGER_FAULT,
        LOCK
    }
}