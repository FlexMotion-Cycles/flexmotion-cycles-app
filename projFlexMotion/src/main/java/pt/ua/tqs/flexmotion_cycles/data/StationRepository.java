package pt.ua.tqs.flexmotion_cycles.data;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.ua.tqs.flexmotion_cycles.domain.Station;

/** Persistence of stations and accessible hubs. */
public interface StationRepository extends JpaRepository<Station, Long> {
}