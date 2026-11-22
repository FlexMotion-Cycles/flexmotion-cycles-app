package pt.ua.tqs.flexmotion_cycles.adapters;

public class InvalidTelemetryMessageException extends RuntimeException {

    public InvalidTelemetryMessageException(String message) {
        super(message);
    }

    public InvalidTelemetryMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
