package pt.ua.tqs.flexmotion_cycles.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUsernameOrderByIdAsc(String username);

    boolean existsByUsername(String username);

    /** The card charged by default; empty when the user has no linked card (BR2). */
    Optional<PaymentMethod> findByUsernameAndDefaultMethodTrue(String username);
}
