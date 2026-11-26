package pt.ua.tqs.flexmotion_cycles.boundary;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Thymeleaf pages of the web UI. The pages hold no business data: their scripts
 * call the REST API, as in docs/architecture.jpeg (Web UI -> HTTP -> REST API).
 */
@Controller
public class WebUiController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Find an e-bike (US-09): stations with free docks and the e-bikes that can be unlocked. */
    @GetMapping("/stations")
    public String stations() {
        return "stations";
    }
}