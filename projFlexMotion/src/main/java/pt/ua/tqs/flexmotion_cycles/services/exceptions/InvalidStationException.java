package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** Thrown when a station registration breaks a rule of US-03. */
public class InvalidStationException extends RuntimeException {

    public InvalidStationException(String message) {
        super(message);
    }
}