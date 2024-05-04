package com.walking.api.repository;

import com.walking.api.data.entity.TrafficApiCall;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficApiCallRepository extends JpaRepository<TrafficApiCall, Long> {

	@Query(
			"select fac from TrafficApiCall fac "
					+ " join fetch fac.traffic "
					+ " where fac.executionNumber = :executionNumber")
	List<TrafficApiCall> findByExecutionNumber(@Param("executionNumber") int executionNumber);
}
