package pt.ua.tqs.flexmotion_cycles.boundary;

import java.security.Principal;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.ua.tqs.flexmotion_cycles.dto.RoleChangeRequest;
import pt.ua.tqs.flexmotion_cycles.dto.UserResponse;
import pt.ua.tqs.flexmotion_cycles.services.UserRoleService;

/** Role management by the platform admin (admins only, see SecurityConfig) (US-02). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRoleService userRoleService;

    public AdminUserController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /** Gives the user a staff role, or END_USER to remove it. */
    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
            @Valid @RequestBody RoleChangeRequest request, Principal principal) {
        return UserResponse.from(
                userRoleService.changeRole(principal.getName(), id, request.role()));
    }
}