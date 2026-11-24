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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


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

    @GetMapping("/{id}")
    public TripDto getTrip(@PathVariable Long id, Principal principal) {
        return TripDto.from(tripService.findTrip(principal.getName(), id));
    }
    
}
