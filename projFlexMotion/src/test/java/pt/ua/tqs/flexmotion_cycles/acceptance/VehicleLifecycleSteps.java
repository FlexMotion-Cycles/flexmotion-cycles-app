package pt.ua.tqs.flexmotion_cycles.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;
import pt.ua.tqs.flexmotion_cycles.domain.exceptions.InvalidStateTransitionException;
import pt.ua.tqs.flexmotion_cycles.services.VehicleService;

public class VehicleLifecycleSteps {
    private final VehicleService vehicleService;
    private final VehicleRepository vehicleRepository;
    private Long vehicleId;
    private RuntimeException error;

    public VehicleLifecycleSteps(VehicleService vehicleService, VehicleRepository vehicleRepository) {
        this.vehicleService = vehicleService;
        this.vehicleRepository = vehicleRepository;
    }

    @Given("a vehicle in state {string}")
    public void aVehicleInState(String state) {
        Vehicle vehicle = new Vehicle("EB-001", VehicleType.E_BIKE, null);
        vehicle.setState(VehicleState.valueOf(state));
        vehicleId = vehicleRepository.save(vehicle).getId();
    }

    @When("the vehicle changes to state {string}")
    public void theVehicleChangesToState(String state) {
        attempt(() -> vehicleService.changeState(vehicleId, VehicleState.valueOf(state)));
    }

    @When("the operator deactivates the vehicle")
    public void theOperatorDeactivatesTheVehicle() {
        attempt(() -> vehicleService.deactivate(vehicleId));
    }

    @When("the operator activates the vehicle")
    public void theOperatorActivatesTheVehicle() {
        attempt(() -> vehicleService.activate(vehicleId));
    }

    @Then("the vehicle is in state {string}")
    public void theVehicleIsInState(String state) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();
        assertEquals(VehicleState.valueOf(state), vehicle.getState());
    }

    @Then("the change is rejected")
    public void theChangeIsRejected() {
        assertInstanceOf(InvalidStateTransitionException.class, error);
    }

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (InvalidStateTransitionException ex) {
            error = ex;
        }
    }
}
