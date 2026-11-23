package pt.ua.tqs.flexmotion_cycles.boundary;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.flexmotion_cycles.services.TripService;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** 202: the unlock command was sent; the trip starts when the lock opens (BR7). */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TripDto unlock(@Valid @RequestBody UnlockRequest request, Principal principal) {
        return TripDto.from(tripService.requestUnlock(principal.getName(), request.vehicleCode()));
    }
}
