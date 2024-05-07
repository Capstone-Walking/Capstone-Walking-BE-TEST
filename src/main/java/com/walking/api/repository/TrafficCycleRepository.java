package com.walking.api.repository;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficCycle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficCycleRepository extends JpaRepository<TrafficCycle, Long> {

	@Query("select tc from TrafficCycle tc where tc.traffic in :traffics")
	List<TrafficCycle> findByTraffics(@Param("traffics") List<Traffic> traffics);

	Optional<TrafficCycle> findByTraffic(Traffic traffic);
}
