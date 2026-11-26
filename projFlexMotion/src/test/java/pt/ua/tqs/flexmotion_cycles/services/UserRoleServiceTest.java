package pt.ua.tqs.flexmotion_cycles.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ua.tqs.flexmotion_cycles.data.UserRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleChangeNotAllowedException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleNotAssignableException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("User role service (US-02)")
class UserRoleServiceTest {

    private static final String ADMIN_EMAIL = "admin@valeclaro.test";
    private static final String OTHER_EMAIL = "sofia@valeclaro.test";
    private static final Long TARGET_ID = 2L;
    private static final Long UNKNOWN_ID = 99L;
    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRoleService userRoleService;

    private User target;

    @BeforeEach
    void setUp() {
        target = new User("Ines", "ines@valeclaro.test", "hashed-password", ADULT_BIRTH_DATE);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OPERATOR", "CLIENT_MANAGER"})
    @Requirement("FM-20")
    void adminAssignsStaffRole(Role staffRole) {
        givenActingUser(ADMIN_EMAIL, Role.ADMIN);
        givenTarget();
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        User changed = userRoleService.changeRole(ADMIN_EMAIL, TARGET_ID, staffRole);

        assertThat(changed.getRole()).isEqualTo(staffRole);
        verify(userRepository).save(target);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"OPERATOR", "CLIENT_MANAGER"})
    @Requirement("FM-20")
    void adminRemovesStaffRoleSoUserIsEndUserAgain(Role staffRole) {
        setRole(target, staffRole);
        givenActingUser(ADMIN_EMAIL, Role.ADMIN);
        givenTarget();
        when(userRepository.save(any(User.class))).then(returnsFirstArg());

        User changed = userRoleService.changeRole(ADMIN_EMAIL, TARGET_ID, Role.END_USER);

        assertThat(changed.getRole()).isEqualTo(Role.END_USER);
        verify(userRepository).save(target);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    @Requirement("FM-20")
    void onlyTheAdminChangesRoles(Role actingRole) {
        givenActingUser(OTHER_EMAIL, actingRole);

        assertThatThrownBy(() -> userRoleService.changeRole(OTHER_EMAIL, TARGET_ID, Role.OPERATOR))
                .isInstanceOf(RoleChangeNotAllowedException.class);
        assertThat(target.getRole()).isEqualTo(Role.END_USER);
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-20")
    void unknownActingAccountCannotChangeRoles() {
        when(userRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.changeRole(OTHER_EMAIL, TARGET_ID, Role.OPERATOR))
                .isInstanceOf(RoleChangeNotAllowedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-20")
    void adminRoleCannotBeAssigned() {
        givenActingUser(ADMIN_EMAIL, Role.ADMIN);

        assertThatThrownBy(() -> userRoleService.changeRole(ADMIN_EMAIL, TARGET_ID, Role.ADMIN))
                .isInstanceOf(RoleNotAssignableException.class);
        assertThat(target.getRole()).isEqualTo(Role.END_USER);
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-20")
    void roleOfAnAdminCannotBeChanged() {
        setRole(target, Role.ADMIN);
        givenActingUser(ADMIN_EMAIL, Role.ADMIN);
        givenTarget();

        assertThatThrownBy(() -> userRoleService.changeRole(ADMIN_EMAIL, TARGET_ID, Role.END_USER))
                .isInstanceOf(RoleNotAssignableException.class);
        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(any());
    }

    @Test
    @Requirement("FM-20")
    void unknownUserIsNotFound() {
        givenActingUser(ADMIN_EMAIL, Role.ADMIN);
        when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userRoleService.changeRole(ADMIN_EMAIL, UNKNOWN_ID, Role.OPERATOR))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    private void givenActingUser(String email, Role role) {
        User acting = new User("Acting", email, "hashed-password", ADULT_BIRTH_DATE);
        setRole(acting, role);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(acting));
    }

    private void givenTarget() {
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
    }

    /** The User has no role setter yet: changeRole is the code under test. */
    private static void setRole(User user, Role role) {
        ReflectionTestUtils.setField(user, "role", role);
    }
}