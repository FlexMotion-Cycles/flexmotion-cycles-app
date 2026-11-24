package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import pt.ua.tqs.fleet_simulator.domain.GeoPoint;

/** A position on the wire, in decimal degrees. */
record PositionMessage(Double latitude, Double longitude) {

    static PositionMessage from(GeoPoint point) {
        return new PositionMessage(point.latitude(), point.longitude());
    }

    GeoPoint toGeoPoint() {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("a position needs latitude and longitude");
        }
        return new GeoPoint(latitude, longitude);
    }
}