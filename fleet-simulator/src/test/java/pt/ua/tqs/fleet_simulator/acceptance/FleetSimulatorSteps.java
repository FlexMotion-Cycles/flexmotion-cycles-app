package pt.ua.tqs.fleet_simulator.acceptance;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.eclipse.paho.client.mqttv3.MqttException;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pt.ua.tqs.fleet_simulator.support.PlatformMqttClient;
import tools.jackson.databind.JsonNode;

/**
 * Steps for fleet_simulator.feature (FM-25). They play the platform and reach the simulator
 * only over MQTT, with the messages of the telemetry contract.
 */
public class FleetSimulatorSteps {

    private static final String COMMAND_TOPIC_PREFIX = "flexmotion/commands/";
    private static final String CONTROL_TOPIC_PREFIX = "flexmotion/simulator/";
    private static final String UNLOCK = "{\"command\": \"UNLOCK\"}";
    private static final String TRIGGER_FAULT =
            "{\"action\": \"TRIGGER_FAULT\", \"faultEvent\": \"%s\"}";

    private PlatformMqttClient platform;

    @Before
    public void connectAsThePlatform() throws MqttException {
        platform = new PlatformMqttClient("platform-acceptance");
    }

    @After
    public void disconnect() throws MqttException {
        platform.close();
    }

    /** The fleet in CucumberSpringConfiguration starts like this; the step checks the reading. */
    @Given("the simulated vehicle {string} is locked with {int} % battery")
    public void theSimulatedVehicleIsLockedWithBattery(String vehicleCode, int batteryPercent)
            throws InterruptedException {
        assertThat(platform.awaitReading(vehicleCode, reading ->
                reading.path("locked").asBoolean()
                        && reading.path("batteryPercent").asInt() == batteryPercent))
                .as("%s locked with %d %% battery", vehicleCode, batteryPercent)
                .isPresent();
    }

    @Given("the platform unlocked {string}")
    public void thePlatformUnlocked(String vehicleCode) throws Exception {
        sendUnlockCommand(vehicleCode);

        assertThat(awaitOpenLock(vehicleCode)).as("%s unlocked", vehicleCode).isPresent();
    }

    /** Readings received before this step are ignored: the next steps read the next round. */
    @When("the next telemetry round is published")
    public void theNextTelemetryRoundIsPublished() {
        platform.clearReadings();
    }

    @When("the platform sends an unlock command for {string}")
    public void thePlatformSendsAnUnlockCommandFor(String vehicleCode) throws MqttException {
        sendUnlockCommand(vehicleCode);
    }

    @When("a test triggers the fault {string} on {string}")
    public void aTestTriggersTheFaultOn(String faultEvent, String vehicleCode)
            throws MqttException {
        platform.publish(CONTROL_TOPIC_PREFIX + vehicleCode,
                String.format(TRIGGER_FAULT, faultEvent));
    }

    @Then("a reading for {string} has its battery, position, lock state and no fault")
    public void aReadingHasItsBatteryPositionLockStateAndNoFault(String vehicleCode)
            throws InterruptedException {
        JsonNode reading = nextReading(vehicleCode);

        assertThat(reading.path("batteryPercent").isInt()).as("batteryPercent").isTrue();
        assertThat(reading.path("position").path("latitude").isNumber()).as("latitude").isTrue();
        assertThat(reading.path("position").path("longitude").isNumber()).as("longitude")
                .isTrue();
        assertThat(reading.path("locked").isBoolean()).as("locked").isTrue();
        assertThat(reading.path("faultEvent").isNull()).as("faultEvent is null").isTrue();
    }

    @Then("a reading for {string} shows the lock open")
    public void aReadingShowsTheLockOpen(String vehicleCode) throws InterruptedException {
        assertThat(awaitOpenLock(vehicleCode)).as("%s reports an open lock", vehicleCode)
                .isPresent();
    }

    @Then("the battery of {string} is below {int} %")
    public void theBatteryIsBelow(String vehicleCode, int batteryPercent)
            throws InterruptedException {
        assertThat(nextReading(vehicleCode).path("batteryPercent").asInt())
                .isLessThan(batteryPercent);
    }

    @Then("the battery of {string} is {int} %")
    public void theBatteryIs(String vehicleCode, int batteryPercent)
            throws InterruptedException {
        assertThat(nextReading(vehicleCode).path("batteryPercent").asInt())
                .isEqualTo(batteryPercent);
    }

    @Then("a reading for {string} reports the fault {string}")
    public void aReadingReportsTheFault(String vehicleCode, String faultEvent)
            throws InterruptedException {
        assertThat(platform.awaitReading(vehicleCode,
                reading -> faultEvent.equals(reading.path("faultEvent").asString())))
                .as("%s reports %s", vehicleCode, faultEvent).isPresent();
    }

    private void sendUnlockCommand(String vehicleCode) throws MqttException {
        platform.publish(COMMAND_TOPIC_PREFIX + vehicleCode, UNLOCK);
    }

    /** A command is answered asynchronously, so a reading of the round may arrive first. */
    private Optional<JsonNode> awaitOpenLock(String vehicleCode) throws InterruptedException {
        return platform.awaitReading(vehicleCode, reading -> !reading.path("locked").asBoolean());
    }

    private JsonNode nextReading(String vehicleCode) throws InterruptedException {
        return platform.awaitReading(vehicleCode, any -> true)
                .orElseThrow(() -> new AssertionError("No reading of " + vehicleCode));
    }
}