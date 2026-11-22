package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.ua.tqs.flexmotion_cycles.boundary.dto.VehicleResponse;
import pt.ua.tqs.flexmotion_cycles.services.VehicleService;

@RestController 
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<VehicleResponse> deactivate(@PathVariable Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<VehicleResponse> activate(@PathVariable Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
