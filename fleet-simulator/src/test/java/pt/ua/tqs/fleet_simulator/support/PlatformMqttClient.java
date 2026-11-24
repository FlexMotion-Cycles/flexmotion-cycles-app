package pt.ua.tqs.fleet_simulator.support;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plays the platform's side of the telemetry contract in tests: it reads every telemetry topic
 * and publishes commands and control messages, as the backend will.
 */
public final class PlatformMqttClient implements AutoCloseable {

    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final String TELEMETRY_TOPIC_PREFIX = "flexmotion/telemetry/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    private final MqttClient client;

    public PlatformMqttClient(String clientId) throws MqttException {
        client = new MqttClient(MqttBroker.url(), clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(MqttBroker.USER);
        options.setPassword(MqttBroker.PASSWORD.toCharArray());
        client.connect(options);
        client.subscribe(TELEMETRY_TOPIC_PREFIX + "+", QOS_AT_LEAST_ONCE, (topic, message) ->
                received.add(new Received(topic,
                        new String(message.getPayload(), StandardCharsets.UTF_8))));
    }

    public void publish(String topic, String payload) throws MqttException {
        client.publish(topic, payload.getBytes(StandardCharsets.UTF_8), QOS_AT_LEAST_ONCE,
                false);
    }

    /** Forgets the readings received so far: the next ones come after this call. */
    public void clearReadings() {
        received.clear();
    }

    /** Waits for a reading of the vehicle that matches the condition; others are skipped. */
    public Optional<JsonNode> awaitReading(String vehicleCode, Predicate<JsonNode> condition)
            throws InterruptedException {
        String topic = TELEMETRY_TOPIC_PREFIX + vehicleCode;
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        for (long left = TIMEOUT.toNanos(); left > 0; left = deadline - System.nanoTime()) {
            Received message = received.poll(left, TimeUnit.NANOSECONDS);
            if (message != null && message.topic().equals(topic)) {
                JsonNode reading = jsonMapper.readTree(message.payload());
                if (condition.test(reading)) {
                    return Optional.of(reading);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void close() throws MqttException {
        client.disconnect();
        client.close();
    }

    private record Received(String topic, String payload) {
    }
}