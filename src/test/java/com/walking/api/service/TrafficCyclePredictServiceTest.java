package com.walking.api.service;

import static org.junit.jupiter.api.Assertions.*;

import com.walking.api.ApiApp;
import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficCycle;
import com.walking.api.repository.TrafficCycleRepository;
import com.walking.api.repository.TrafficRepository;
import com.walking.api.service.dto.PredictData;
import java.util.ArrayList;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ActiveProfiles(value = "test")
@SpringBootTest
@ContextConfiguration(classes = {ApiApp.class})
class TrafficCyclePredictServiceTest {

	@Autowired TrafficRepository trafficRepository;
	@Autowired TrafficCycleRepository trafficCycleRepository;
	@Autowired TrafficCyclePredictService trafficCyclePredictService;

	private static final int BIAS = 1;

	@ParameterizedTest(name = "{0} 신호등에 대해 {1} 간격으로 데이터를 조회하며 사이클을 계산할 수 있습니다")
	@MethodSource("predictableTraffics")
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

	static Stream<Arguments> predictableTraffics() {
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

	@ParameterizedTest(name = "{0} 신호등은 예측할 수 없어 Optional.empty()를 반환합니다")
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

	@ParameterizedTest(name = "{0}은 사이클을 예측하였고, {1}은 사이클 예측에 실패하였습니다.")
	@MethodSource("mixedTraffics")
	void 사이클을_계산할_수_없는_신호등과_계산가능한_신호등이_혼재_되어있어도_잘_계산합니다(
			List<Long> predictableIds, List<Long> unpredictableIds, int interval) {
		List<Long> trafficIds = new ArrayList<>();
		trafficIds.addAll(predictableIds);
		trafficIds.addAll(unpredictableIds);
		List<Traffic> traffics = trafficRepository.findByIds(trafficIds);
		Map<Traffic, PredictData> result = trafficCyclePredictService.execute(traffics, interval);

		for (Traffic traffic : result.keySet()) {
			PredictData predictData = result.get(traffic);
			TrafficCycle trafficCycle = trafficCycleRepository.findByTraffic(traffic).orElseThrow();

			// 예측이 불가능한 신호등인 경우
			if (unpredictableIds.contains(traffic.getId())) {
				Assertions.assertThat(predictData.getRedCycle()).isEqualTo(Optional.empty());
				Assertions.assertThat(predictData.getGreenCycle()).isEqualTo(Optional.empty());

				log.debug(
						(traffic + "은 ===> " + predictData.getGreenCycle() + ", " + predictData.getRedCycle())
								+ "일 것이다.");
			} else { // 예측이 가능한 신호등인 경우
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
	}

	static Stream<Arguments> mixedTraffics() {
		List<Long> predictableIds = Arrays.asList(1L, 9L, 8L, 2L);
		List<Long> unpredictableIds = Arrays.asList(3L);

		return Stream.of(Arguments.of(predictableIds, unpredictableIds, 5));
	}
}
