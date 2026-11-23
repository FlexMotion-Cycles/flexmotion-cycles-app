package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleRequest;
import pt.ua.tqs.flexmotion_cycles.dto.VehicleResponse;
import pt.ua.tqs.flexmotion_cycles.services.VehicleService;

/** REST resource for fleet vehicles (operators only, see SecurityConfig). */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /** Registers a vehicle at its current station. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse registerVehicle(@Valid @RequestBody VehicleRequest request) {
        return VehicleResponse.from(vehicleService.registerVehicle(request));
    }

    /** Takes an AVAILABLE vehicle out of service (moves it to MAINTENANCE). */
    @PatchMapping("/{id}/deactivate")
    public VehicleResponse deactivate(@PathVariable Long id) {
        return VehicleResponse.from(vehicleService.deactivate(id));
    }

    /** Puts a vehicle in MAINTENANCE back into service (moves it to AVAILABLE). */
    @PatchMapping("/{id}/activate")
    public VehicleResponse activate(@PathVariable Long id) {
        return VehicleResponse.from(vehicleService.activate(id));
    }
}
