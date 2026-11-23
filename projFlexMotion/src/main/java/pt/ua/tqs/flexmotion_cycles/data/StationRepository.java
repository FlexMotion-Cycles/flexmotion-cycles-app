package pt.ua.tqs.flexmotion_cycles.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.tqs.flexmotion_cycles.domain.Station;
import pt.ua.tqs.flexmotion_cycles.domain.StationType;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByType(StationType type);
}
