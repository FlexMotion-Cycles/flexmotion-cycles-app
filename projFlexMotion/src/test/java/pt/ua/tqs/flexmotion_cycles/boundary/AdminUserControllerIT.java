package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.data.UserRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminUserControllerIT {

    private static final String ROLE_URL = "/api/admin/users/{id}/role";
    private static final String ADMIN_EMAIL = "admin@valeclaro.test";
    private static final String INES_EMAIL = "ines@valeclaro.test";
    private static final String NO_ACCOUNT_EMAIL = "ghost@valeclaro.test";
    private static final long UNKNOWN_ID = 999_999L;
    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);

    private final MockMvc mockMvc;
    private final UserRepository userRepository;

    private User ines;

    @Autowired
    AdminUserControllerIT(MockMvc mockMvc, UserRepository userRepository) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        saveUser("Admin", ADMIN_EMAIL, Role.ADMIN);
        ines = saveUser("Ines", INES_EMAIL, Role.END_USER);
    }

    @Test
    @Requirement("FM-20")
    void adminGivesStaffRoleThatIsStored() throws Exception {
        changeRoleAs(ADMIN_EMAIL, ines.getId(), Role.OPERATOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPERATOR"));

        assertThat(storedRoleOf(ines)).isEqualTo(Role.OPERATOR);
    }

    @Test
    @Requirement("FM-20")
    void adminRemovesStaffRole() throws Exception {
        ines.changeRole(Role.CLIENT_MANAGER);
        ines = userRepository.save(ines);

        changeRoleAs(ADMIN_EMAIL, ines.getId(), Role.END_USER).andExpect(status().isOk());

        assertThat(storedRoleOf(ines)).isEqualTo(Role.END_USER);
    }

    @Test
    @Requirement("FM-20")
    void adminRoleWithoutAnAdminAccountIsForbidden() throws Exception {
        changeRoleAs(NO_ACCOUNT_EMAIL, ines.getId(), Role.OPERATOR)
                .andExpect(status().isForbidden());

        assertThat(storedRoleOf(ines)).isEqualTo(Role.END_USER);
    }

    @Test
    @Requirement("FM-20")
    void adminRoleCannotBeAssigned() throws Exception {
        changeRoleAs(ADMIN_EMAIL, ines.getId(), Role.ADMIN).andExpect(status().isBadRequest());

        assertThat(storedRoleOf(ines)).isEqualTo(Role.END_USER);
    }

    @Test
    @Requirement("FM-20")
    void unknownUserReturnsNotFound() throws Exception {
        changeRoleAs(ADMIN_EMAIL, UNKNOWN_ID, Role.OPERATOR).andExpect(status().isNotFound());
    }

    private ResultActions changeRoleAs(String actingEmail, long userId, Role role)
            throws Exception {
        return mockMvc.perform(put(ROLE_URL, userId)
                .with(user(actingEmail).roles(Role.ADMIN.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"" + role.name() + "\"}"));
    }

    private User saveUser(String name, String email, Role role) {
        User user = new User(name, email, "hashed-password", ADULT_BIRTH_DATE);
        user.changeRole(role);
        return userRepository.save(user);
    }

    private Role storedRoleOf(User user) {
        return userRepository.findById(user.getId()).orElseThrow().getRole();
    }
}