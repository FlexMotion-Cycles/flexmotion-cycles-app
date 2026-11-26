package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;
import pt.ua.tqs.flexmotion_cycles.services.UserRoleService;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleChangeNotAllowedException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.RoleNotAssignableException;
import pt.ua.tqs.flexmotion_cycles.services.exceptions.UserNotFoundException;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    private static final String ROLE_URL = "/api/admin/users/{id}/role";
    private static final String ADMIN_EMAIL = "admin@valeclaro.test";
    private static final String INES_EMAIL = "ines@valeclaro.test";
    private static final Long USER_ID = 2L;
    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);

    private final MockMvc mockMvc;

    @MockitoBean
    private UserRoleService userRoleService;

    @Autowired
    AdminUserControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @Requirement("FM-20")
    void adminGivesUserAStaffRole() throws Exception {
        User ines = new User("Ines", INES_EMAIL, "hashed-password", ADULT_BIRTH_DATE);
        ines.changeRole(Role.OPERATOR);
        when(userRoleService.changeRole(ADMIN_EMAIL, USER_ID, Role.OPERATOR)).thenReturn(ines);

        changeRole("{\"role\": \"OPERATOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(INES_EMAIL))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @Requirement("FM-20")
    void accountThatIsNotAdminIsForbidden() throws Exception {
        when(userRoleService.changeRole(ADMIN_EMAIL, USER_ID, Role.OPERATOR))
                .thenThrow(new RoleChangeNotAllowedException());

        changeRole("{\"role\": \"OPERATOR\"}").andExpect(status().isForbidden());
    }

    @Test
    @Requirement("FM-20")
    void roleOutsideStaffRolesIsRejected() throws Exception {
        when(userRoleService.changeRole(ADMIN_EMAIL, USER_ID, Role.ADMIN))
                .thenThrow(new RoleNotAssignableException("Role ADMIN cannot be assigned"));

        changeRole("{\"role\": \"ADMIN\"}").andExpect(status().isBadRequest());
    }

    @Test
    @Requirement("FM-20")
    void unknownUserReturnsNotFound() throws Exception {
        when(userRoleService.changeRole(ADMIN_EMAIL, USER_ID, Role.OPERATOR))
                .thenThrow(new UserNotFoundException(USER_ID));

        changeRole("{\"role\": \"OPERATOR\"}").andExpect(status().isNotFound());
    }

    @Test
    @Requirement("FM-20")
    void missingRoleIsRejected() throws Exception {
        changeRole("{}").andExpect(status().isBadRequest());

        verifyNoInteractions(userRoleService);
    }

    @Test
    @Requirement("FM-20")
    void unknownRoleIsRejected() throws Exception {
        changeRole("{\"role\": \"SUPERUSER\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(userRoleService);
    }

    private ResultActions changeRole(String body) throws Exception {
        return mockMvc.perform(put(ROLE_URL, USER_ID)
                .with(user(ADMIN_EMAIL).roles(Role.ADMIN.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}