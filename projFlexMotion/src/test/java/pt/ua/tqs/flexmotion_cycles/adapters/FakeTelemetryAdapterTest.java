package pt.ua.tqs.flexmotion_cycles.adapters;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.adapters.telemetry.FakeTelemetryAdapter;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.ports.LockClosedEvent;

@ExtendWith(MockitoExtension.class)
public class FakeTelemetryAdapterTest {
    private static final Instant CLOSED_AT = Instant.parse("2026-11-22T10:20:00Z");
    private static final GeoPoint CENTRAL = new GeoPoint(40.6405, -8.6538);

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FakeTelemetryAdapter telemetry;

    @BeforeEach
    void setUp() {
        telemetry = new FakeTelemetryAdapter(eventPublisher, Clock.fixed(CLOSED_AT, ZoneOffset.UTC), false);
    }

    @Test
    @Requirement("FM-28")
    void reportingALockClosedPublishesTheEventWithTheReadingTimeAndPosition() {
        telemetry.reportLockClosed("EB-01", CLOSED_AT, CENTRAL);

        verify(eventPublisher).publishEvent(new LockClosedEvent("EB-01", CLOSED_AT, CENTRAL));
    }

    @Test
    @Requirement("FM-28")
    void lockClosedWithoutPositionIsRejectedAndNotPublished() {
        assertThatThrownBy(() -> telemetry.reportLockClosed("EB-01", CLOSED_AT, null)).isInstanceOf(NullPointerException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
