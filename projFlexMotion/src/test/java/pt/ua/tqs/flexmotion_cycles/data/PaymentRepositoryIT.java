package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.Payment;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentMethod;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentStatus;
import pt.ua.tqs.flexmotion_cycles.domain.PaymentType;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;
import pt.ua.tqs.flexmotion_cycles.domain.Trip;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PaymentRepositoryIT {

    private static final Instant REQUESTED_AT = Instant.parse("2026-11-22T10:00:00Z");
    private static final BigDecimal DEPOSIT = new BigDecimal("10.00");

    private final PaymentRepository paymentRepository;
    private final TestEntityManager entityManager;

    private Station central;

    @Autowired
    PaymentRepositoryIT(PaymentRepository paymentRepository, TestEntityManager entityManager) {
        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void setUp() {
        central = entityManager.persist(
                new Station("Central", 40.6405, -8.6538, 10, StationType.CONVENTIONAL));
    }

    @Test
    @Requirement("FM-32")
    void findsTheDepositOfTheUsersTripOnTheVehicle() {
        Payment deposit = persistDeposit("ines", "EB-01", PaymentStatus.AUTHORIZED);
        persistDeposit("rui", "EB-02", PaymentStatus.AUTHORIZED);
        entityManager.clear();

        assertThat(paymentRepository.findByTypeAndTripUsernameAndTripVehicleCode(
                PaymentType.DEPOSIT, "ines", "EB-01"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getId()).isEqualTo(deposit.getId());
                    assertThat(found.getAmount()).isEqualByComparingTo(DEPOSIT);
                    assertThat(found.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
                });
        assertThat(paymentRepository.findByTypeAndTripUsernameAndTripVehicleCode(
                PaymentType.DEPOSIT, "ines", "EB-02")).isEmpty();
    }

    @Test
    @Requirement("FM-32")
    void tellsWhetherTheUserHasADepositStillHeld() {
        persistDeposit("ines", "EB-01", PaymentStatus.DECLINED);
        persistDeposit("rui", "EB-02", PaymentStatus.AUTHORIZED);

        assertThat(paymentRepository.existsByTypeAndStatusAndTripUsername(
                PaymentType.DEPOSIT, PaymentStatus.AUTHORIZED, "ines")).isFalse();
        assertThat(paymentRepository.existsByTypeAndStatusAndTripUsername(
                PaymentType.DEPOSIT, PaymentStatus.AUTHORIZED, "rui")).isTrue();
    }

    private Payment persistDeposit(String username, String vehicleCode, PaymentStatus status) {
        Vehicle vehicle = entityManager.persist(
                new Vehicle(vehicleCode, VehicleType.E_BIKE, 80, central));
        Trip trip = entityManager.persist(new Trip(username, vehicle, REQUESTED_AT));
        PaymentMethod card = entityManager.persist(
                new PaymentMethod(username, "pm_gateway_" + vehicleCode, "4242"));
        Payment deposit = new Payment(PaymentType.DEPOSIT, DEPOSIT, card, trip, REQUESTED_AT);
        if (status == PaymentStatus.AUTHORIZED) {
            deposit.markAuthorized("pi_" + vehicleCode);
        } else {
            deposit.markDeclined();
        }
        return entityManager.persistFlushFind(deposit);
    }
}
