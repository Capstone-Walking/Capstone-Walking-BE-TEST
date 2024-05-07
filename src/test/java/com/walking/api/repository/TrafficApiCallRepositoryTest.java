package com.walking.api.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficApiCall;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ContextConfiguration(classes = {TrafficRepository.class, TrafficApiCallRepository.class})
class TrafficApiCallRepositoryTest extends RepositoryTest {

	@Autowired TrafficRepository trafficRepository;
	@Autowired TrafficApiCallRepository trafficApiCallRepository;

	@Test
	void 최근_데이터_가져오기() {
		List<Long> ids = Arrays.asList(1L, 4L, 7L);
		List<Traffic> traffics = trafficRepository.findByIds(ids);
		List<TrafficApiCall> recentlyData = trafficApiCallRepository.getRecentlyData(traffics, 3, 6);
		for (TrafficApiCall recentlyDatum : recentlyData) {
			log.debug(recentlyDatum.toString());
		}
	}
}
