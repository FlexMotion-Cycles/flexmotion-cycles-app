package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class TelemetryFixtures {

    private TelemetryFixtures() {
    }

    static String read(String name) throws IOException {
        try (InputStream in = TelemetryFixtures.class.getResourceAsStream("/telemetry/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
