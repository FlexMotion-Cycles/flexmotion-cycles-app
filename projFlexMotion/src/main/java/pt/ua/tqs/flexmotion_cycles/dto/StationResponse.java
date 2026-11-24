package pt.ua.tqs.flexmotion_cycles.dto;

import java.time.LocalTime;

import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;

/** Station data returned by the API. */
public record StationResponse(
        Long id,
        String name,
        double latitude,
        double longitude,
        StationType type,
        int dockCapacity,
        Double overflowRadiusMeters,
        Boolean stepFreeAccess,
        LocalTime technicianFrom,
        LocalTime technicianTo) {

    /** Maps a station entity to the data the API exposes. */
    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getLocation().latitude(),
                station.getLocation().longitude(),
                station.getType(),
                station.getDockCapacity(),
                station.getOverflowRadiusMeters(),
                station.getStepFreeAccess(),
                station.getTechnicianFrom(),
                station.getTechnicianTo());
    }
}