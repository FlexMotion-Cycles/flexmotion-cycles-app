package pt.ua.tqs.flexmotion_cycles.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** The payment of this type for the user's trip on the vehicle (e.g. its deposit, BR16). */
    Optional<Payment> findByTypeAndTripUsernameAndTripVehicleCode(PaymentType type,
            String username, String vehicleCode);

    /** Whether the user has a payment of this type in this state (e.g. a deposit still held). */
    boolean existsByTypeAndStatusAndTripUsername(PaymentType type, PaymentStatus status,
            String username);
}
