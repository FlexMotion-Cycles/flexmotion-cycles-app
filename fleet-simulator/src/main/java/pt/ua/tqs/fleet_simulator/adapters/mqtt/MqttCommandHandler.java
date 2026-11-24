package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pt.ua.tqs.fleet_simulator.config.MqttConfig;
import pt.ua.tqs.fleet_simulator.config.MqttProperties;
import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.GeoPoint;
import pt.ua.tqs.fleet_simulator.services.FleetSimulator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Applies the commands and control messages that reach the simulator. The vehicle code comes
 * from the topic. A message that breaks the contract is logged and discarded, and the
 * simulator keeps listening.
 */
@Component
public class MqttCommandHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MqttCommandHandler.class);

    private final FleetSimulator fleetSimulator;
    private final JsonMapper jsonMapper;
    private final String commandTopicPrefix;
    private final String controlTopicPrefix;

    public MqttCommandHandler(FleetSimulator fleetSimulator, JsonMapper jsonMapper,
            MqttProperties properties) {
        this.fleetSimulator = fleetSimulator;
        this.jsonMapper = jsonMapper;
        this.commandTopicPrefix =
                properties.commandTopicPrefix() + MqttConfig.TOPIC_LEVEL_SEPARATOR;
        this.controlTopicPrefix =
                properties.controlTopicPrefix() + MqttConfig.TOPIC_LEVEL_SEPARATOR;
    }

    public void handle(String topic, String payload) {
        try {
            if (topic != null && topic.startsWith(commandTopicPrefix)) {
                applyCommand(vehicleCode(topic, commandTopicPrefix), payload);
            } else if (topic != null && topic.startsWith(controlTopicPrefix)) {
                applyControl(vehicleCode(topic, controlTopicPrefix), payload);
            } else {
                throw new IllegalArgumentException("unexpected topic");
            }
        } catch (JacksonException | IllegalArgumentException e) {
            LOG.warn("Discarding an invalid message on {}: {}", topic, e.getMessage());
        }
    }

    private static String vehicleCode(String topic, String prefix) {
        String vehicleCode = topic.substring(prefix.length());
        if (vehicleCode.isEmpty()) {
            throw new IllegalArgumentException("the topic has no vehicle code");
        }
        return vehicleCode;
    }

    private void applyCommand(String vehicleCode, String payload) {
        CommandMessage message = jsonMapper.readValue(payload, CommandMessage.class);
        if (message == null || message.command() == null) {
            throw new IllegalArgumentException("the command is missing");
        }
        LOG.info("{} command for {}", message.command(), vehicleCode);
        fleetSimulator.unlock(vehicleCode);
    }

    private void applyControl(String vehicleCode, String payload) {
        ControlMessage message = jsonMapper.readValue(payload, ControlMessage.class);
        if (message == null || message.action() == null) {
            throw new IllegalArgumentException("the action is missing");
        }
        switch (message.action()) {
            case TRIGGER_FAULT -> triggerFault(vehicleCode, message.faultEvent());
            case LOCK -> lock(vehicleCode, message.position());
        }
    }

    private void triggerFault(String vehicleCode, FaultEvent fault) {
        if (fault == null) {
            throw new IllegalArgumentException("TRIGGER_FAULT needs a faultEvent");
        }
        LOG.info("Triggering {} on {}", fault, vehicleCode);
        fleetSimulator.triggerFault(vehicleCode, fault);
    }

    private void lock(String vehicleCode, PositionMessage position) {
        if (position == null) {
            LOG.info("Locking {} where it is", vehicleCode);
            fleetSimulator.lock(vehicleCode);
        } else {
            GeoPoint dockPosition = position.toGeoPoint();
            LOG.info("Locking {} at {}", vehicleCode, dockPosition);
            fleetSimulator.lockAt(vehicleCode, dockPosition);
        }
    }
}