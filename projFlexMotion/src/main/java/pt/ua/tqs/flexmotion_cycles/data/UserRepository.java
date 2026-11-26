package pt.ua.tqs.flexmotion_cycles.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
}
