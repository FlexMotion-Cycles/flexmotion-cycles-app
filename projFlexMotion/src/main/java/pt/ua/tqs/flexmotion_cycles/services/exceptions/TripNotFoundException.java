package pt.ua.tqs.flexmotion_cycles.services.exceptions;

public class TripNotFoundException extends RuntimeException{

    public TripNotFoundException(Long tripId) {
        super("Trip " + tripId + " not found");
    }
    
}
