package pt.ua.tqs.flexmotion_cycles.boundary;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.flexmotion_cycles.services.StationAvailabilityService;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationAvailabilityService stationAvailabilityService;

    public StationController(StationAvailabilityService stationAvailabilityService) {
        this.stationAvailabilityService = stationAvailabilityService;
    }

    /** Conventional stations with their free docks and the e-bikes that can be unlocked. */
    @GetMapping
    public List<StationAvailabilityDto> listConventionalStations() {
        return stationAvailabilityService.findConventionalStations().stream()
                .map(StationAvailabilityDto::from)
                .toList();
    }
}
