package pt.ua.tqs.flexmotion_cycles.boundary;

import java.util.List;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.services.StationAvailability;

public record StationAvailabilityDto(Long id, String name, double latitude, double longitude,
        long freeDocks, List<AvailableVehicleDto> availableEbikes) {

    static StationAvailabilityDto from(StationAvailability availability) {
        Station station = availability.station();
        return new StationAvailabilityDto(station.getId(), station.getName(),
                station.getLatitude(), station.getLongitude(), availability.freeDocks(),
                availability.availableVehicles().stream().map(AvailableVehicleDto::from).toList());
    }
}
