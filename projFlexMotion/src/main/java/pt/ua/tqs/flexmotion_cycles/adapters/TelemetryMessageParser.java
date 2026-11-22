package pt.ua.tqs.flexmotion_cycles.adapters;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import pt.ua.tqs.flexmotion_cycles.domain.TelemetryReading;

@Component
public class TelemetryMessageParser {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public TelemetryMessageParser(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public TelemetryReading parse(String payload) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
