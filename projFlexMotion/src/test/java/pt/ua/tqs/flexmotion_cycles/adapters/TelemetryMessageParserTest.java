package pt.ua.tqs.flexmotion_cycles.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Instant;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

class TelemetryMessageParserTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final TelemetryMessageParser parser = new TelemetryMessageParser(
            jsonMapper, Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    @Requirement("FM-26")
    void parsesValidReadingWithoutFault() throws IOException {
        TelemetryReading reading = parser.parse(TelemetryFixtures.read("valid-reading.json"));

        assertThat(reading).isEqualTo(new TelemetryReading("EB-0001",
                Instant.parse("2026-11-22T16:05:00Z"), 19,
                new GeoPoint(40.6405, -8.6538), true, null));
    }

    @Test
    @Requirement("FM-26")
    void parsesFaultEvent() throws IOException {
        TelemetryReading reading =
                parser.parse(TelemetryFixtures.read("valid-reading-with-fault.json"));

        assertThat(reading.faultEvent()).isEqualTo("BRAKE_FAILURE");
    }

    @Test
    @Requirement("FM-26")
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{not json"))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"vehicleCode", "timestamp", "batteryPercent", "position", "locked"})
    @Requirement("FM-26")
    void rejectsMissingRequiredField(String field) throws IOException {
        ObjectNode message = validMessage();
        message.remove(field);

        assertThatThrownBy(() -> parser.parse(message.toString()))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    @Test
    @Requirement("FM-26")
    void rejectsTimestampNotInIso8601() throws IOException {
        ObjectNode message = validMessage().put("timestamp", "22/11/2026 16:05");

        assertThatThrownBy(() -> parser.parse(message.toString()))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    @Requirement("FM-26")
    void rejectsBatteryOutsideZeroToHundred(int batteryPercent) throws IOException {
        ObjectNode message = validMessage().put("batteryPercent", batteryPercent);

        assertThatThrownBy(() -> parser.parse(message.toString()))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100})
    @Requirement("FM-26")
    void acceptsBatteryAtZeroAndHundred(int batteryPercent) throws IOException {
        ObjectNode message = validMessage().put("batteryPercent", batteryPercent);

        assertThat(parser.parse(message.toString()).batteryPercent()).isEqualTo(batteryPercent);
    }

    @Test
    @Requirement("FM-26")
    void rejectsLatitudeOutOfRange() throws IOException {
        ObjectNode message = validMessage();
        ((ObjectNode) message.get("position")).put("latitude", 91.0);

        assertThatThrownBy(() -> parser.parse(message.toString()))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    @Test
    @Requirement("FM-26")
    void rejectsLongitudeOutOfRange() throws IOException {
        ObjectNode message = validMessage();
        ((ObjectNode) message.get("position")).put("longitude", 181.0);

        assertThatThrownBy(() -> parser.parse(message.toString()))
                .isInstanceOf(InvalidTelemetryMessageException.class);
    }

    private ObjectNode validMessage() throws IOException {
        return (ObjectNode) jsonMapper.readTree(TelemetryFixtures.read("valid-reading.json"));
    }
}
