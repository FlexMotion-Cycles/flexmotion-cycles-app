package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.tqs.flexmotion_cycles.dto.StationRequest;
import pt.ua.tqs.flexmotion_cycles.dto.StationResponse;
import pt.ua.tqs.flexmotion_cycles.services.StationService;

/** REST resource for stations and accessible hubs. */
@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    /** Registers a station (operators only). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse registerStation(@Valid @RequestBody StationRequest request) {
        return StationResponse.from(stationService.registerStation(request));
    }
}