package pt.ua.tqs.flexmotion_cycles.data;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import pt.ua.tqs.flexmotion_cycles.TestcontainersConfiguration;
import pt.ua.tqs.flexmotion_cycles.domain.GeoPoint;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class StationRepositoryIT {

    private static final GeoPoint VALE_CLARO_CENTRE = new GeoPoint(40.6405, -8.6538);

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @Requirement("FM-21")
    void savesConventionalStationWithLocationAndOverflowZone() {
        Station saved = stationRepository.save(
                Station.conventional("Central Square", VALE_CLARO_CENTRE, 20, 10.0));
        entityManager.flush();
        entityManager.clear();

        Station found = stationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getType()).isEqualTo(StationType.CONVENTIONAL);
        assertThat(found.getName()).isEqualTo("Central Square");
        assertThat(found.getLocation()).isEqualTo(VALE_CLARO_CENTRE);
        assertThat(found.getDockCapacity()).isEqualTo(20);
        assertThat(found.getOverflowRadiusMeters()).isEqualTo(10.0);
        assertThat(found.getTechnicianFrom()).isNull();
    }

    @Test
    @Requirement("FM-21")
    void savesAccessibleHubWithStepFreeAccessAndTechnicianHours() {
        Station saved = stationRepository.save(Station.accessible("Marginal Accessible Hub",
                VALE_CLARO_CENTRE, 6, true, LocalTime.of(9, 0), LocalTime.of(18, 0)));
        entityManager.flush();
        entityManager.clear();

        Station found = stationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getType()).isEqualTo(StationType.ACCESSIBLE);
        assertThat(found.getStepFreeAccess()).isTrue();
        assertThat(found.getTechnicianFrom()).isEqualTo(LocalTime.of(9, 0));
        assertThat(found.getTechnicianTo()).isEqualTo(LocalTime.of(18, 0));
        assertThat(found.getOverflowRadiusMeters()).isNull();
    }
}