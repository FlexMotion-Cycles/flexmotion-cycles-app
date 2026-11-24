package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** Thrown when another vehicle is already registered with the same identifier code. */
public class DuplicateVehicleCodeException extends RuntimeException {

    public DuplicateVehicleCodeException(String code) {
        super("A vehicle with code " + code + " is already registered");
    }
}