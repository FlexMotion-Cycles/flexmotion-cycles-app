package pt.ua.tqs.flexmotion_cycles.boundary;

import java.time.Instant;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.TripStatus;

public record TripDto(Long id, String vehicleCode, TripStatus status, Instant requestedAt,
        Instant startedAt) {

    static TripDto from(Trip trip) {
        return new TripDto(trip.getId(), trip.getVehicle().getCode(), trip.getStatus(),
                trip.getRequestedAt(), trip.getStartedAt());
    }
}
