package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.GeoPoint;
import pt.ua.tqs.fleet_simulator.domain.TelemetryReading;
import tools.jackson.databind.json.JsonMapper;

class MqttTelemetryPublisherTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-11-23T10:15:30Z");
    private static final GeoPoint CENTRAL_SQUARE = new GeoPoint(40.6405, -8.6538);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final QueueChannel channel = new QueueChannel();
    private final MqttTelemetryPublisher publisher =
            new MqttTelemetryPublisher(channel, jsonMapper, ContractExamples.MQTT_PROPERTIES);

    @Test
    @Requirement("FM-25")
    void readingIsWrittenAsInTheContractExample() {
        publisher.publish(new TelemetryReading("EB-0001", TIMESTAMP, 80, CENTRAL_SQUARE, true, null));

        assertThat(jsonMapper.readTree(payloadOf(nextMessage())))
                .isEqualTo(jsonMapper.readTree(ContractExamples.read("telemetry-reading.json")));
    }

    @Test
    @Requirement("FM-25")
    void readingGoesToTheTelemetryTopicOfItsVehicle() {
        publisher.publish(new TelemetryReading("CB-0001", TIMESTAMP, 80, CENTRAL_SQUARE, true, null));

        assertThat(nextMessage().getHeaders().get(MqttHeaders.TOPIC))
                .isEqualTo("flexmotion/telemetry/CB-0001");
    }

    @ParameterizedTest
    @ValueSource(strings = {"BRAKE_FAILURE", "FLAT_TYRE", "MOTOR_FAILURE"})
    @Requirement("FM-25")
    void faultIsWrittenWithItsContractCode(String faultCode) {
        FaultEvent fault = FaultEvent.valueOf(faultCode);

        publisher.publish(new TelemetryReading("EB-0001", TIMESTAMP, 80, CENTRAL_SQUARE, false, fault));

        assertThat(jsonMapper.readTree(payloadOf(nextMessage())).get("faultEvent").asString())
                .isEqualTo(faultCode);
    }

    @Test
    @Requirement("FM-25")
    void readingThatCannotBeDeliveredDoesNotStopTheSimulation() {
        MessageChannel unreachableBroker = (message, timeout) -> {
            throw new MessageDeliveryException(message, "broker unreachable");
        };
        MqttTelemetryPublisher failingPublisher = new MqttTelemetryPublisher(
                unreachableBroker, jsonMapper, ContractExamples.MQTT_PROPERTIES);
        TelemetryReading reading =
                new TelemetryReading("EB-0001", TIMESTAMP, 80, CENTRAL_SQUARE, true, null);

        assertThatCode(() -> failingPublisher.publish(reading)).doesNotThrowAnyException();
    }

    private Message<?> nextMessage() {
        Message<?> message = channel.receive(0);
        assertThat(message).as("published message").isNotNull();
        return message;
    }

    private static String payloadOf(Message<?> message) {
        return (String) message.getPayload();
    }
}