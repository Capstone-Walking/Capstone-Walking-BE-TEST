package com.walking.api.repository;

import com.walking.api.data.entity.Traffic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficRepository extends JpaRepository<Traffic, Long> {

	@Query("select t from Traffic t where t.id in :ids")
	List<Traffic> findByIds(@Param("ids") List<Long> ids);
}
