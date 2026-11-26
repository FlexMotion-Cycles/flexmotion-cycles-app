package pt.ua.tqs.flexmotion_cycles.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.junit.jupiter.api.Test;

/** Every API feature against every role: only the owner role gets through (US-02). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoleAccessIT {

    private static final long ANY_ID = 999L;
    private static final String ANY_ACCOUNT = "someone@valeclaro.test";
    private static final String EMPTY_JSON = "{}";

    /** Each feature of the API and the only role that may use it. */
    enum Feature {
        SEARCH_STATIONS(HttpMethod.GET, "/api/stations", Role.END_USER),
        REGISTER_STATION(HttpMethod.POST, "/api/stations", Role.OPERATOR),
        REGISTER_VEHICLE(HttpMethod.POST, "/api/vehicles", Role.OPERATOR),
        DEACTIVATE_VEHICLE(HttpMethod.PATCH, "/api/vehicles/" + ANY_ID + "/deactivate",
                Role.OPERATOR),
        ACTIVATE_VEHICLE(HttpMethod.PATCH, "/api/vehicles/" + ANY_ID + "/activate",
                Role.OPERATOR),
        UNLOCK_VEHICLE(HttpMethod.POST, "/api/trips", Role.END_USER),
        VIEW_TRIP(HttpMethod.GET, "/api/trips/" + ANY_ID, Role.END_USER),
        CHANGE_USER_ROLE(HttpMethod.PUT, "/api/admin/users/" + ANY_ID + "/role", Role.ADMIN);

        private final HttpMethod method;
        private final String path;
        private final Role owner;

        Feature(HttpMethod method, String path, Role owner) {
            this.method = method;
            this.path = path;
            this.owner = owner;
        }
    }

    private final MockMvc mockMvc;

    @Autowired
    RoleAccessIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    static List<Arguments> ownerOfEachFeature() {
        List<Arguments> cases = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            cases.add(Arguments.of(feature, feature.owner));
        }
        return cases;
    }

    static List<Arguments> otherRolesOfEachFeature() {
        List<Arguments> cases = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            for (Role role : Role.values()) {
                if (role != feature.owner) {
                    cases.add(Arguments.of(feature, role));
                }
            }
        }
        return cases;
    }

    @ParameterizedTest(name = "{1} uses {0}")
    @MethodSource("ownerOfEachFeature")
    @Requirement("FM-20")
    void roleReachesItsOwnFeature(Feature feature, Role role) throws Exception {
        int status = perform(feature, role).andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }

    @ParameterizedTest(name = "{1} is forbidden from {0}")
    @MethodSource("otherRolesOfEachFeature")
    @Requirement("FM-20")
    void requestToAnotherRolesFeatureIsForbidden(Feature feature, Role role) throws Exception {
        perform(feature, role).andExpect(status().isForbidden());
    }

    @Test
    @Requirement("FM-20")
    void stationSearchIsNoLongerPublic() throws Exception {
        mockMvc.perform(get(Feature.SEARCH_STATIONS.path))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions perform(Feature feature, Role role) throws Exception {
        return mockMvc.perform(request(feature.method, feature.path)
                .with(user(ANY_ACCOUNT).roles(role.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPTY_JSON));
    }
}