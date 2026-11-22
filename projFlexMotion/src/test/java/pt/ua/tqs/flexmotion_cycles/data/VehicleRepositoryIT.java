package pt.ua.tqs.flexmotion_cycles.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.Vehicle;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleState;
import pt.ua.tqs.flexmotion_cycles.domain.VehicleType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class VehicleRepositoryIT {

    private static final GeoPoint VALE_CLARO_CENTRE = new GeoPoint(40.6405, -8.6538);

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Station centralSquare;
    private Station riverside;

    @BeforeEach
    void setUp() {
        centralSquare = entityManager.persist(
                Station.conventional("Central Square", VALE_CLARO_CENTRE, 20, 10.0));
        riverside = entityManager.persist(
                Station.conventional("Riverside", VALE_CLARO_CENTRE, 20, 10.0));
    }

    @Test
    @Requirement("FM-21")
    void savesNewVehicleAsAvailableAtItsStation() {
        Vehicle saved = vehicleRepository.save(
                new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));
        entityManager.flush();
        entityManager.clear();

        Vehicle found = vehicleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCode()).isEqualTo("EB-0001");
        assertThat(found.getType()).isEqualTo(VehicleType.E_BIKE);
        assertThat(found.getState()).isEqualTo(VehicleState.AVAILABLE);
        assertThat(found.getStation().getId()).isEqualTo(centralSquare.getId());
    }

    @Test
    @Requirement("FM-21")
    void countsOnlyVehiclesDockedAtTheGivenStation() {
        entityManager.persist(new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));
        entityManager.persist(new Vehicle("EB-0002", VehicleType.E_BIKE, centralSquare));
        entityManager.persist(new Vehicle("CB-0001", VehicleType.CARGO_E_BIKE, riverside));
        entityManager.flush();

        assertThat(vehicleRepository.countByStationId(centralSquare.getId())).isEqualTo(2);
        assertThat(vehicleRepository.countByStationId(riverside.getId())).isEqualTo(1);
    }

    @Test
    @Requirement("FM-21")
    void findsWhetherAVehicleCodeIsAlreadyRegistered() {
        entityManager.persistAndFlush(new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));

        assertThat(vehicleRepository.existsByCode("EB-0001")).isTrue();
        assertThat(vehicleRepository.existsByCode("EB-0002")).isFalse();
    }

    @Test
    @Requirement("FM-21")
    void rejectsTwoVehiclesWithTheSameCode() {
        vehicleRepository.saveAndFlush(new Vehicle("EB-0001", VehicleType.E_BIKE, centralSquare));
        Vehicle duplicate = new Vehicle("EB-0001", VehicleType.HANDBIKE, riverside);

        assertThatThrownBy(() -> vehicleRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}