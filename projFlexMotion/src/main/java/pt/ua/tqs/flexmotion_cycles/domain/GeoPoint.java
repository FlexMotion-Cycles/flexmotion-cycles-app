package pt.ua.tqs.flexmotion_cycles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GeoPoint(
        @Column(nullable = false) double latitude,
        @Column(nullable = false) double longitude) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public double distanceMetersTo(GeoPoint other) {
        double deltaLatitude = Math.toRadians(other.latitude - latitude);
        double deltaLongitude = Math.toRadians(other.longitude - longitude);
        double haversine = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }
}
