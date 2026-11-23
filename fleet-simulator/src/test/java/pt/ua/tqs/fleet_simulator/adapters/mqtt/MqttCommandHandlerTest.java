package pt.ua.tqs.fleet_simulator.adapters.mqtt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.mockito.junit.jupiter.MockitoExtension;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.fleet_simulator.domain.FaultEvent;
import pt.ua.tqs.fleet_simulator.domain.GeoPoint;
import pt.ua.tqs.fleet_simulator.services.FleetSimulator;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MqttCommandHandlerTest {

    private static final String COMMAND_TOPIC = "flexmotion/commands/EB-0001";
    private static final String CONTROL_TOPIC = "flexmotion/simulator/EB-0001";

    @Mock
    private FleetSimulator fleetSimulator;

    private MqttCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MqttCommandHandler(fleetSimulator, JsonMapper.builder().build(),
                ContractExamples.MQTT_PROPERTIES);
    }

    @Test
    @Requirement("FM-25")
    void unlockCommandUnlocksTheVehicleOfTheTopic() {
        handler.handle(COMMAND_TOPIC, ContractExamples.read("unlock-command.json"));

        verify(fleetSimulator).unlock("EB-0001");
    }

    @Test
    @Requirement("FM-25")
    void triggerFaultMakesTheVehicleReportTheFault() {
        handler.handle(CONTROL_TOPIC, ContractExamples.read("trigger-fault-control.json"));

        verify(fleetSimulator).triggerFault("EB-0001", FaultEvent.BRAKE_FAILURE);
    }

    @Test
    @Requirement("FM-25")
    void lockWithPositionDocksTheVehicleThere() {
        handler.handle(CONTROL_TOPIC, ContractExamples.read("lock-control.json"));

        verify(fleetSimulator).lockAt("EB-0001", new GeoPoint(40.6405, -8.6538));
    }

    @Test
    @Requirement("FM-25")
    void lockWithoutPositionLocksTheVehicleWhereItIs() {
        handler.handle(CONTROL_TOPIC, "{\"action\": \"LOCK\"}");

        verify(fleetSimulator).lock("EB-0001");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {
        "not json",
        "null",
        "{}",
        "{\"command\": \"LOCK\"}",
        "{\"command\": \"unlock\"}"
    })
    @Requirement("FM-25")
    void invalidCommandIsDiscarded(String payload) {
        handler.handle(COMMAND_TOPIC, payload);

        verifyNoInteractions(fleetSimulator);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {
        "not json",
        "null",
        "{}",
        "{\"action\": \"EXPLODE\"}",
        "{\"action\": \"TRIGGER_FAULT\"}",
        "{\"action\": \"TRIGGER_FAULT\", \"faultEvent\": \"LOW_BATTERY\"}",
        "{\"action\": \"LOCK\", \"position\": {\"latitude\": 40.6405}}",
        "{\"action\": \"LOCK\", \"position\": {\"longitude\": -8.6538}}"
    })
    @Requirement("FM-25")
    void invalidControlMessageIsDiscarded(String payload) {
        handler.handle(CONTROL_TOPIC, payload);

        verifyNoInteractions(fleetSimulator);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "flexmotion/telemetry/EB-0001",
        "flexmotion/commands",
        "flexmotion/commands/",
        "flexmotion/simulator/"
    })
    @Requirement("FM-25")
    void messageOnAnUnexpectedTopicIsDiscarded(String topic) {
        handler.handle(topic, ContractExamples.read("unlock-command.json"));

        verifyNoInteractions(fleetSimulator);
    }
}