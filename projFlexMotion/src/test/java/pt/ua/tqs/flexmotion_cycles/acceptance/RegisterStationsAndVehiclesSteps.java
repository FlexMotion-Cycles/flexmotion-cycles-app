package pt.ua.tqs.flexmotion_cycles.acceptance;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pt.ua.tqs.flexmotion_cycles.data.StationRepository;
import pt.ua.tqs.flexmotion_cycles.data.VehicleRepository;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

/** Steps for US-03. Each scenario gets a new instance, so the fields never leak between them. */
public class RegisterStationsAndVehiclesSteps {

    private static final String STATIONS_URL = "/api/stations";
    private static final String VEHICLES_URL = "/api/vehicles";
    private static final GeoPoint VALE_CLARO_CENTRE = new GeoPoint(40.6405, -8.6538);
    private static final double OVERFLOW_RADIUS_METERS = 10.0;

    private final MockMvc mockMvc;
    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;

    private final Map<String, Long> stationIdsByName = new HashMap<>();
    private RequestPostProcessor currentUser;
    private ResultActions response;
    private Long requestedStationId;

    public RegisterStationsAndVehiclesSteps(MockMvc mockMvc,
            StationRepository stationRepository, VehicleRepository vehicleRepository) {
        this.mockMvc = mockMvc;
        this.stationRepository = stationRepository;
        this.vehicleRepository = vehicleRepository;
    }

    // Login (JWT) arrives with US-01/US-02; until then the operator is simulated.
    @Given("I am logged in as an operator")
    public void iAmLoggedInAsAnOperator() {
        currentUser = user("sofia").roles("OPERATOR");
    }

    @Given("a conventional station {string} with {int} docks and {int} vehicles docked")
    public void aConventionalStationWithVehiclesDocked(String name, int docks, int docked) {
        Station station = stationRepository.save(
                Station.conventional(name, VALE_CLARO_CENTRE, docks, OVERFLOW_RADIUS_METERS));
        stationIdsByName.put(name, station.getId());
        vehicleRepository.saveAll(IntStream.rangeClosed(1, docked)
                .mapToObj(n -> new Vehicle("EB-%04d".formatted(n), VehicleType.E_BIKE, station))
                .toList());
    }

    @When("I register a conventional station {string} with {int} docks")
    public void iRegisterAConventionalStation(String name, int docks) throws Exception {
        registerStation("""
                {
                  "name": "%s",
                  "latitude": 40.6405,
                  "longitude": -8.6538,
                  "type": "CONVENTIONAL",
                  "dockCapacity": %d
                }
                """.formatted(name, docks));
    }

    @When("I register an accessible station {string} with {int} docks, step-free access "
            + "and technician hours from {string} to {string}")
    public void iRegisterAnAccessibleStation(String name, int docks, String from, String to)
            throws Exception {
        registerStation("""
                {
                  "name": "%s",
                  "latitude": 40.6420,
                  "longitude": -8.6500,
                  "type": "ACCESSIBLE",
                  "dockCapacity": %d,
                  "stepFreeAccess": true,
                  "technicianFrom": "%s",
                  "technicianTo": "%s"
                }
                """.formatted(name, docks, from, to));
    }

    @When("I register an accessible station {string} with {int} docks, step-free access "
            + "and no technician hours")
    public void iRegisterAnAccessibleStationWithoutTechnicianHours(String name, int docks)
            throws Exception {
        registerStation("""
                {
                  "name": "%s",
                  "latitude": 40.6420,
                  "longitude": -8.6500,
                  "type": "ACCESSIBLE",
                  "dockCapacity": %d,
                  "stepFreeAccess": true
                }
                """.formatted(name, docks));
    }

    @When("I register the vehicle {string} of type {string} at {string}")
    public void iRegisterTheVehicle(String code, String type, String stationName)
            throws Exception {
        requestedStationId = stationIdsByName.get(stationName);
        response = mockMvc.perform(post(VEHICLES_URL)
                .with(currentUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "code": "%s",
                          "type": "%s",
                          "stationId": %d
                        }
                        """.formatted(code, type, requestedStationId)));
    }

    @Then("the station is registered")
    public void theStationIsRegistered() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Then("its overflow zone has a radius of {int} metres")
    public void itsOverflowZoneHasARadiusOf(int metres) throws Exception {
        response.andExpect(jsonPath("$.overflowRadiusMeters").value((double) metres));
    }

    @Then("the registration is accepted")
    public void theRegistrationIsAccepted() throws Exception {
        response.andExpect(status().isCreated());
    }

    @Then("the registration is refused")
    public void theRegistrationIsRefused() throws Exception {
        response.andExpect(status().isBadRequest());
    }

    @Then("the vehicle is registered as {string}")
    public void theVehicleIsRegisteredAs(String state) throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value(state))
                .andExpect(jsonPath("$.stationId").value(requestedStationId));
    }

    @Then("the registration is refused because the station is full")
    public void theRegistrationIsRefusedBecauseTheStationIsFull() throws Exception {
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Station " + requestedStationId + " has no free dock"));
    }

    private void registerStation(String json) throws Exception {
        response = mockMvc.perform(post(STATIONS_URL)
                .with(currentUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}