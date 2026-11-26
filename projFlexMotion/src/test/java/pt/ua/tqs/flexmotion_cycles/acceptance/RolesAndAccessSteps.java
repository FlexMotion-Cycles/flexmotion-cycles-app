package pt.ua.tqs.flexmotion_cycles.acceptance;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pt.ua.tqs.flexmotion_cycles.data.UserRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;

/**
 * Steps for US-02. Accounts are stored in the database; the logged-in user is simulated
 * until US-01 brings the login.
 */
public class RolesAndAccessSteps {

    private static final String ROLE_URL = "/api/admin/users/{id}/role";
    private static final String TEST_NAME = "Test user";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);
    private static final String ANY_ACCOUNT = "someone@valeclaro.test";
    private static final String EMPTY_JSON = "{}";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;

    private String adminEmail;
    private RequestPostProcessor currentUser;
    private ResultActions response;

    public RolesAndAccessSteps(MockMvc mockMvc, UserRepository userRepository) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
    }

    /** Only the US-02 scenarios store accounts, so only they start from an empty table. */
    @Before("@FM-20")
    public void clearAccounts() {
        userRepository.deleteAll();
    }

    @Given("an admin account {string}")
    public void anAdminAccount(String email) {
        saveAccount(email, Role.ADMIN);
        adminEmail = email;
    }

    @Given("an end user account {string}")
    public void anEndUserAccount(String email) {
        saveAccount(email, Role.END_USER);
    }

    @Given("an operator account {string}")
    public void anOperatorAccount(String email) {
        saveAccount(email, Role.OPERATOR);
    }

    @Given("a user account {string} without an assigned role")
    public void aUserAccountWithoutAnAssignedRole(String email) {
        userRepository.save(new User(TEST_NAME, email, PASSWORD_HASH, ADULT_BIRTH_DATE));
    }

    @Given("{string} has the role {word}")
    public void accountHasTheRole(String email, String role) {
        User account = findAccount(email);
        account.changeRole(Role.valueOf(role));
        userRepository.save(account);
    }

    @Given("a user with the role {word}")
    public void aUserWithTheRole(String role) {
        currentUser = user(ANY_ACCOUNT).roles(role);
    }

    @When("the admin assigns the role {word} to {string}")
    public void theAdminAssignsTheRole(String role, String email) throws Exception {
        assignRole(adminEmail, role, email);
    }

    @When("{string} assigns the role {word} to {string}")
    public void accountAssignsTheRole(String actingEmail, String role, String email)
            throws Exception {
        assignRole(actingEmail, role, email);
    }

    @When("that user calls {word} {word}")
    public void thatUserCalls(String method, String path) throws Exception {
        response = mockMvc.perform(request(HttpMethod.valueOf(method), path)
                .with(currentUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content(EMPTY_JSON));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) throws Exception {
        response.andExpect(status().is(expectedStatus));
    }

    @Then("the role of {string} is {word}")
    public void theRoleOfAccountIs(String email, String role) {
        assertThat(findAccount(email).getRole()).isEqualTo(Role.valueOf(role));
    }

    /** The acting user logs in with the role stored in their account. */
    private void assignRole(String actingEmail, String role, String email) throws Exception {
        Role actingRole = findAccount(actingEmail).getRole();
        response = mockMvc.perform(put(ROLE_URL, findAccount(email).getId())
                .with(user(actingEmail).roles(actingRole.name()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"" + role + "\"}"));
    }

    private void saveAccount(String email, Role role) {
        User account = new User(TEST_NAME, email, PASSWORD_HASH, ADULT_BIRTH_DATE);
        account.changeRole(role);
        userRepository.save(account);
    }

    private User findAccount(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }
}