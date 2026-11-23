package pt.ua.tqs.flexmotion_cycles.adapters;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

@ExtendWith(MockitoExtension.class)
class MqttTelemetryAdapterTest {

    private static final String VALID_PAYLOAD = "valid";
    private static final String INVALID_PAYLOAD = "invalid";
    private static final TelemetryReading READING = new TelemetryReading("EB-0001",
            Instant.parse("2026-11-22T16:05:00Z"), 19, new GeoPoint(40.6405, -8.6538),
            true, null);

    @Mock
    private TelemetryMessageParser parser;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MqttTelemetryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MqttTelemetryAdapter(parser, eventPublisher);
    }

    @Test
    @Requirement("FM-26")
    void publishesValidReadingAsEvent() {
        when(parser.parse(VALID_PAYLOAD)).thenReturn(READING);

        adapter.handle(VALID_PAYLOAD);

        verify(eventPublisher).publishEvent(READING);
    }

    @Test
    @Requirement("FM-26")
    void discardsInvalidMessageWithoutThrowing() {
        when(parser.parse(INVALID_PAYLOAD))
                .thenThrow(new InvalidTelemetryMessageException("Malformed telemetry message"));

        assertThatCode(() -> adapter.handle(INVALID_PAYLOAD)).doesNotThrowAnyException();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @Requirement("FM-26")
    void keepsConsumingAfterInvalidMessage() {
        when(parser.parse(INVALID_PAYLOAD))
                .thenThrow(new InvalidTelemetryMessageException("Malformed telemetry message"));
        when(parser.parse(VALID_PAYLOAD)).thenReturn(READING);

        adapter.handle(INVALID_PAYLOAD);
        adapter.handle(VALID_PAYLOAD);

        verify(eventPublisher, times(1)).publishEvent(READING);
    }
}
