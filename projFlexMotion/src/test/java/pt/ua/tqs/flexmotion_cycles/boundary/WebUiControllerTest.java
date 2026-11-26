package pt.ua.tqs.flexmotion_cycles.boundary;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.config.SecurityConfig;

/** The web UI pages render for anonymous visitors (no credentials sent). */
@WebMvcTest(WebUiController.class)
@Import(SecurityConfig.class)
class WebUiControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    WebUiControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @Requirement("FM-54")
    void homePageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("<title>FlexMotion Cycles</title>")));
    }

    @Test
    @Requirement("FM-54")
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("Log in")));
    }

    @Test
    @Requirement("FM-54")
    void findAnEbikePageIsPublic() throws Exception {
        mockMvc.perform(get("/stations"))
                .andExpect(status().isOk())
                .andExpect(view().name("stations"))
                .andExpect(content().string(containsString("Find an e-bike")));
    }

    @Test
    @Requirement("FM-54")
    void otherPathsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized());
    }
}