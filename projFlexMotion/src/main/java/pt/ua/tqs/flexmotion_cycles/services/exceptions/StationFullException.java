package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** Thrown when a vehicle would be placed at a station with no free dock (US-03). */
public class StationFullException extends RuntimeException {

    public StationFullException(Long stationId) {
        super("Station " + stationId + " has no free dock");
    }
}