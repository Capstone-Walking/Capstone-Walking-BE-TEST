package com.walking.api.service;

import static org.junit.jupiter.api.Assertions.*;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficCycle;
import com.walking.api.repository.TrafficCycleRepository;
import com.walking.api.repository.TrafficRepository;
import com.walking.api.service.dto.PredictData;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ContextConfiguration(classes = {TrafficRepository.class, TrafficCyclePredictService.class})
class TrafficCyclePredictServiceTest extends RepositoryTest {

	@Autowired TrafficRepository trafficRepository;
	@Autowired TrafficCycleRepository trafficCycleRepository;
	@Autowired TrafficCyclePredictService trafficCyclePredictService;

	private static final int BIAS = 1;

	@ParameterizedTest(name = "{0} 신호등에 대해 {1} 간격으로 데이터를 조회하며 사이클을 계산할 수 있습니다")
	@MethodSource("generateData")
	void 신호등에_대한_사이클을_예측할_수_있습니다(List<Long> trafficIds, int interval) {
		// given
		List<Traffic> traffics = trafficRepository.findByIds(trafficIds);

		Map<Traffic, PredictData> result = trafficCyclePredictService.execute(traffics, interval);

		for (Traffic traffic : result.keySet()) {
			PredictData predictData = result.get(traffic);
			TrafficCycle trafficCycle = trafficCycleRepository.findByTraffic(traffic).orElseThrow();

			// 계산된 사이클과 실제 사이클을 비교
			assertTrue(
					predictData.getGreenCycle().get() < trafficCycle.getGreenCycle() + BIAS
							&& predictData.getGreenCycle().get() > trafficCycle.getGreenCycle() - BIAS
							&& predictData.getRedCycle().get() < trafficCycle.getRedCycle() + BIAS
							&& predictData.getRedCycle().get() > trafficCycle.getRedCycle() - BIAS);

			log.debug(
					(traffic + "은 ===> " + predictData.getGreenCycle() + ", " + predictData.getRedCycle())
							+ "일 것이다.");
		}
	}

	@ParameterizedTest(name = "{0} 신호등은 예측할 수 없어 Optinal.empty()를 반환합니다")
	@ValueSource(longs = {3L})
	void 사이클을_계산할_수_없는_신호등은_Optional_Empty를_반환합니다(long unpredictableTrafficId) {
		List<Traffic> unpredictableTraffics =
				trafficRepository.findByIds(Arrays.asList(unpredictableTrafficId));
		Map<Traffic, PredictData> result = trafficCyclePredictService.execute(unpredictableTraffics, 5);

		for (Traffic unpredictableTraffic : result.keySet()) {
			PredictData unpredictableData = result.get(unpredictableTraffic);
			Assertions.assertThat(unpredictableData.getRedCycle()).isEqualTo(Optional.empty());
			Assertions.assertThat(unpredictableData.getGreenCycle()).isEqualTo(Optional.empty());
		}
	}

	static Stream<Arguments> generateData() {
		int dataInterval = 5;

		List<Long> trafficIds01 = Arrays.asList(1L, 2L, 4L, 5L);
		List<Long> trafficIds02 = Arrays.asList(6L, 7L, 8L);
		List<Long> trafficIds03 = Arrays.asList(9L, 10L);
		List<Long> trafficIds04 = Arrays.asList(1L, 2L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

		return Stream.of(
				Arguments.of(trafficIds01, dataInterval),
				Arguments.of(trafficIds02, dataInterval - 3),
				Arguments.of(trafficIds03, dataInterval + 3),
				Arguments.of(trafficIds04, dataInterval));
	}
}
