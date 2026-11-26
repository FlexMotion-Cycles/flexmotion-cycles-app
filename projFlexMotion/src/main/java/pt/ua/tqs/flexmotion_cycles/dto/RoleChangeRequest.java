package pt.ua.tqs.flexmotion_cycles.dto;

import jakarta.validation.constraints.NotNull;
import pt.ua.tqs.flexmotion_cycles.domain.Role;

/** Role the admin gives to a user; END_USER removes a staff role (US-02). */
public record RoleChangeRequest(@NotNull Role role) {
    
}
