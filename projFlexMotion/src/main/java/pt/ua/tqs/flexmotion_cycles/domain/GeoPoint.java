package pt.ua.tqs.flexmotion_cycles.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record GeoPoint(
        @Column(nullable = false) double latitude,
        @Column(nullable = false) double longitude) {

    public double distanceMetersTo(GeoPoint other) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
