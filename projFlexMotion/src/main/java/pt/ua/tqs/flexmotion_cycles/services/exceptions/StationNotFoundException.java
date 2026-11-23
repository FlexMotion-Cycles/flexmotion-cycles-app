package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** Thrown when a request refers to a station that does not exist. */
public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(Long stationId) {
        super("Station " + stationId + " does not exist");
    }
}