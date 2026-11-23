package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleResponse;
import pt.ua.tqs.flexmotion_cycles.services.VehicleService;

/** REST resource for fleet vehicles. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /** Registers a vehicle at its current station (operators only). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse registerVehicle(@Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(vehicleService.registerVehicle(request));
    }
}