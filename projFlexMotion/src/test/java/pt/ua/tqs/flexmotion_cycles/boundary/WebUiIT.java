package pt.ua.tqs.flexmotion_cycles.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;

/** Web UI served by the running application to an anonymous browser (no credentials). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class WebUiIT {

    private final int port;

    @Autowired
    WebUiIT(@Value("${local.server.port}") int port) {
        this.port = port;
    }

    @Test
    @Requirement("FM-54")
    void findAnEbikePageOpensWithoutLogin() {
        given().port(port)
                .when().get("/stations")
                .then().statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("Find an e-bike"));
    }

    @Test
    @Requirement("FM-54")
    void missingStaticFileIsNotFoundInsteadOfUnauthorized() {
        given().port(port)
                .when().get("/css/missing.css")
                .then().statusCode(404);
    }
}