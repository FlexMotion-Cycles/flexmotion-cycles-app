package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.ua.tqs.flexmotion_cycles.domain.exceptions.InvalidStateTransitionException;
import pt.ua.tqs.flexmotion_cycles.services.UnlockRefusedException;
import pt.ua.tqs.flexmotion_cycles.services.VehicleNotFoundException;

import pt.ua.tqs.flexmotion_cycles.services.exceptions.DuplicateVehicleCodeException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.InvalidStationException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationFullException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.StationNotFoundException;

/** Maps service exceptions to HTTP status codes (RFC 9457 problem details). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    public ProblemDetail handleNotFound(VehicleNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UnlockRefusedException.class)
    public ProblemDetail handleUnlockRefused(UnlockRefusedException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentChange(OptimisticLockingFailureException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The vehicle was changed by another request, please try again");
    }
  
    @ExceptionHandler(InvalidStationException.class)
    ProblemDetail handleInvalidStation(InvalidStationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(StationNotFoundException.class)
    ProblemDetail handleStationNotFound(StationNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({StationFullException.class, DuplicateVehicleCodeException.class,
            InvalidStateTransitionException.class})
    ProblemDetail handleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}