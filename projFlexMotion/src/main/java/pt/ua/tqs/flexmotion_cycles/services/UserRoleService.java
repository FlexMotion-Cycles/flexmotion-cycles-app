package pt.ua.tqs.flexmotion_cycles.services;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ua.tqs.flexmotion_cycles.data.UserRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleChangeNotAllowedException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleNotAssignableException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.UserNotFoundException;

/** Role management by the platform admin (US-02). */
@Service
public class UserRoleService {
    private static final Set<Role> ASSIGNABLE_ROLES =
            EnumSet.of(Role.END_USER, Role.OPERATOR, Role.CLIENT_MANAGER);
    private final UserRepository userRepository;

    public UserRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Gives the user the new role on behalf of the acting account. END_USER removes a
     * staff role.
     *
     * @throws RoleChangeNotAllowedException if the acting account is not an admin
     * @throws RoleNotAssignableException if the change is outside OPERATOR and CLIENT_MANAGER
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    public User changeRole(String actingEmail, Long userId, Role newRole) {
        boolean actingIsAdmin = userRepository.findByEmail(actingEmail)
                .map(acting -> acting.getRole() == Role.ADMIN)
                .orElse(false);
        if (!actingIsAdmin) {
            throw new RoleChangeNotAllowedException();
        }
        if (!ASSIGNABLE_ROLES.contains(newRole)) {
            throw new RoleNotAssignableException("Role " + newRole + " cannot be assigned");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() == Role.ADMIN) {
            throw new RoleNotAssignableException("The role of an admin cannot be changed");
        }
        user.changeRole(newRole);
        return userRepository.save(user);
    }
}