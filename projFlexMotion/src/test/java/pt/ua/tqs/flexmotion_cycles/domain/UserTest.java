package pt.ua.tqs.flexmotion_cycles.domain;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

class UserTest {

    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);

    @Test
    @Requirement("FM-20")
    void newAccountStartsAsEndUser() {
        User user = new User("Rui", "rui@valeclaro.test", "hashed-password", ADULT_BIRTH_DATE);

        assertThat(user.getRole()).isEqualTo(Role.END_USER);
    }

    @Test
    @Requirement("FM-20")
    void platformHasExactlyTheFourRolesOfTheStory() {
        assertThat(Role.values()).containsExactlyInAnyOrder(
                Role.END_USER, Role.OPERATOR, Role.CLIENT_MANAGER, Role.ADMIN);
    }
}