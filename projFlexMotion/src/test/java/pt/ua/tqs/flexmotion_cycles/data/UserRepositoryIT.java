package pt.ua.tqs.flexmotion_cycles.data;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.Role;
import pt.ua.tqs.flexmotion_cycles.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryIT {

    private static final LocalDate ADULT_BIRTH_DATE = LocalDate.of(1995, 3, 14);
    private static final String INES_EMAIL = "ines@valeclaro.test";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @Requirement("FM-20")
    void newAccountIsStoredAsEndUser() {
        User saved = userRepository.save(newUser(INES_EMAIL));
        entityManager.flush();
        entityManager.clear();

        User found = userRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getRole()).isEqualTo(Role.END_USER);
        assertThat(found.getEmail()).isEqualTo(INES_EMAIL);
    }

    @Test
    @Requirement("FM-20")
    void findsAccountByEmail() {
        userRepository.save(newUser(INES_EMAIL));
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail(INES_EMAIL))
                .get()
                .extracting(User::getEmail)
                .isEqualTo(INES_EMAIL);
    }

    @Test
    @Requirement("FM-20")
    void unknownEmailFindsNoAccount() {
        assertThat(userRepository.findByEmail("nobody@valeclaro.test")).isEmpty();
    }

    @Test
    @Requirement("FM-20")
    void rejectsSecondAccountWithSameEmail() {
        userRepository.saveAndFlush(newUser(INES_EMAIL));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser(INES_EMAIL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User newUser(String email) {
        return new User("Ines", email, "hashed-password", ADULT_BIRTH_DATE);
    }
}