package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class GeoPointTest {

    private static final double TOLERANCE_METERS = 0.1;

    @Test
    @Requirement("FM-26")
    void distanceToSamePointIsZero() {
        GeoPoint point = new GeoPoint(40.6405, -8.6538);

        assertThat(point.distanceMetersTo(point)).isZero();
    }

    @Test
    @Requirement("FM-26")
    void distanceBetweenNearbyPointsIsInMeters() {
        GeoPoint from = new GeoPoint(40.0, -8.0);
        GeoPoint to = new GeoPoint(40.0001, -8.0);

        assertThat(from.distanceMetersTo(to)).isCloseTo(11.12, within(TOLERANCE_METERS));
    }
}
