package pt.ua.tqs.flexmotion_cycles.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import org.junit.jupiter.api.Test;

class GeoPointTest {

    private static final GeoPoint CENTRAL = new GeoPoint(40.6405, -8.6538);
    private static final double TOLERANCE_METERS = 0.1;

    @Test
    @Requirement("FM-28")
    void samePointIsAtZeroMetres() {
        assertThat(CENTRAL.distanceMetersTo(new GeoPoint(40.6405, -8.6538))).isZero();
    }

    @Test
    @Requirement("FM-28")
    void tenThousandthOfADegreeOfLatitudeIsAboutElevenMetres() {
        GeoPoint north = new GeoPoint(40.6406, -8.6538);

        assertThat(CENTRAL.distanceMetersTo(north)).isCloseTo(11.12, within(0.01));
    }

    @Test
    @Requirement("FM-28")
    void distanceIsTheSameInBothDirections() {
        GeoPoint other = new GeoPoint(40.6500, -8.6500);

        assertThat(CENTRAL.distanceMetersTo(other)).isCloseTo(other.distanceMetersTo(CENTRAL), within(1e-9));
    }


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