package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
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
        TelemetryMessage message = read(payload);
        Set<ConstraintViolation<TelemetryMessage>> violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new InvalidTelemetryMessageException(
                    "Invalid telemetry message: " + describe(violations));
        }
        return message.toReading();
    }

    private TelemetryMessage read(String payload) {
        try {
            return jsonMapper.readValue(payload, TelemetryMessage.class);
        } catch (JacksonException e) {
            throw new InvalidTelemetryMessageException("Malformed telemetry message", e);
        }
    }

    private static String describe(Set<ConstraintViolation<TelemetryMessage>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
