package pt.ua.tqs.flexmotion_cycles.boundary;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.ua.tqs.flexmotion_cycles.services.StationAvailabilityService;
import pt.ua.tqs.flexmotion_cycles.dto.StationRequest;
import pt.ua.tqs.flexmotion_cycles.dto.StationResponse;
import pt.ua.tqs.flexmotion_cycles.services.StationService;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationAvailabilityService stationAvailabilityService;
    private final StationService stationService;

    public StationController(StationAvailabilityService stationAvailabilityService, StationService stationService) {
        this.stationAvailabilityService = stationAvailabilityService;
        this.stationService = stationService;
    }
  
    /** Registers a station (operators only). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse registerStation(@Valid @RequestBody StationRequest request) {
        return StationResponse.from(stationService.registerStation(request));
    }

    /** Conventional stations with their free docks and the e-bikes that can be unlocked. */
    @GetMapping
    public List<StationAvailabilityDto> listConventionalStations() {
        return stationAvailabilityService.findConventionalStations().stream()
                .map(StationAvailabilityDto::from)
                .toList();
    }
}