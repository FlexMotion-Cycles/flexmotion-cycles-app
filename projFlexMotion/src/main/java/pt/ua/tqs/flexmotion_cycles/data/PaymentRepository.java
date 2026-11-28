package pt.ua.tqs.flexmotion_cycles.data;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
