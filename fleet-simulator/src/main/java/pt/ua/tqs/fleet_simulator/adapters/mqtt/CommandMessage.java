package pt.ua.tqs.fleet_simulator.adapters.mqtt;

/** A command from the platform. */
record CommandMessage(Command command) {

    /** UNLOCK is the only command in version 1 of the contract. */
    enum Command {
        UNLOCK
    }
}