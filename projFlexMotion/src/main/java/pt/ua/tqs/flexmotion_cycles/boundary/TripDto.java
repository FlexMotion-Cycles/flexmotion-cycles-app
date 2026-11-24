package pt.ua.tqs.flexmotion_cycles.boundary;

import java.time.Instant;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;

public record TripDto(Long id, String vehicleCode, TripStatus status, Instant requestedAt,
        Instant startedAt, Instant endedAt, String endStationName, TripCostDto cost) {

    static TripDto from(Trip trip) {
        String endStationName = trip.getEndStation() == null ? null : trip.getEndStation().getName();
        return new TripDto(trip.getId(), trip.getVehicle().getCode(), trip.getStatus(),
                trip.getRequestedAt(), trip.getStartedAt(), trip.getEndedAt(), endStationName,
                TripCostDto.from(trip.getCost()));
    }
}
