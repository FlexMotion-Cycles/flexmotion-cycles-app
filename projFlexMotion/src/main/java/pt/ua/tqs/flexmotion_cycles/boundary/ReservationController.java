package pt.ua.tqs.flexmotion_cycles.boundary;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.flexmotion_cycles.services.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** 201: the e-bike is held for the user until expiresAt (BR5). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto reserve(@Valid @RequestBody ReservationRequest request,
            Principal principal) {
        return ReservationDto.from(
                reservationService.reserve(principal.getName(), request.vehicleCode()));
    }

    @GetMapping("/{id}")
    public ReservationDto getReservation(@PathVariable Long id, Principal principal) {
        return ReservationDto.from(reservationService.findReservation(principal.getName(), id));
    }
}
