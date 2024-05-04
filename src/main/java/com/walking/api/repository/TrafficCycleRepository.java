package com.walking.api.repository;

import com.walking.api.data.entity.TrafficCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficCycleRepository extends JpaRepository<TrafficCycle, Long> {}
