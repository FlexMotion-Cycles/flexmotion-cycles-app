package pt.ua.tqs.flexmotion_cycles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GeoPoint(
        @Column(nullable = false) double latitude,
        @Column(nullable = false) double longitude) {
        private static final double EARTH_RADIUS_METERS = 6371000;

        public double distanceMetersTo(GeoPoint other) {
                double fromLatitude = Math.toRadians(latitude);
                double toLatitude = Math.toRadians(other.latitude);
                double deltaLatitude = toLatitude - fromLatitude;
                double deltaLongitude = Math.toRadians(other.longitude - longitude);

                // Haversine Formule
                double haversine = Math.pow(Math.sin(deltaLatitude / 2), 2) + Math.cos(fromLatitude) * Math.cos(toLatitude) * Math.pow(Math.sin(deltaLongitude / 2), 2);

                return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
        }
}