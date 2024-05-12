package com.walking.api.service.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.walking.api.data.constant.TrafficColor;
import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficApiCall;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class PredictDatumPredictorTest {

	private static Traffic traffic = new Traffic(1L);

	@ParameterizedTest(name = "{1}s 간격으로 데이터를 조회하며 간격의 초록불({2}s) or 빨간불({3}s)을 예측할 수 있습니다")
	@MethodSource("predictableTraffics")
	void predictableCycle(
			List<TrafficApiCall> trafficApiCalls,
			int interval,
			Float greenExpected,
			Float redExpected,
			boolean expectedPredictedGreenCycle,
			boolean expectedPredictedRedCycle) {
		// given
		PredictDatum predictDatum = new PredictDatum(traffic);
		PredictDatumPredictor predictDatumPredictor =
				new PredictDatumPredictor(predictDatum, trafficApiCalls, interval);

		// when
		Float greenCycle = predictDatumPredictor.predictGreenCycle();
		if (greenCycle != null) {
			predictDatum.loadGreenCycle(greenCycle);
		}
		Float redCycle = predictDatumPredictor.predictRedCycle();
		if (redCycle != null) {
			predictDatum.loadRedCycle(redCycle);
		}

		// then
		if (expectedPredictedGreenCycle) {
			assertTrue(predictDatum.isPredictedGreenCycle());
			assertEquals(greenExpected, greenCycle);
			log.debug(">>> greenCycle: " + greenCycle);
		} else if (expectedPredictedRedCycle) {
			assertTrue(predictDatum.isPredictedRedCycle());
			assertEquals(redExpected, redCycle);
			log.debug(">>> redCycle: " + redCycle);
		} else {
			assertFalse(predictDatum.isPredictedGreenCycle());
			assertFalse(predictDatum.isPredictedRedCycle());
			assertNull(greenCycle);
			assertNull(redCycle);
			log.debug(">>> not predicted");
		}
	}

	private static Stream<Arguments> predictableTraffics() {
		Arguments arguments0 = getPredictableRedGreenArguments(10F, 10F, 20);
		Arguments arguments1 = getPredictableRedGreenArguments(15F, 10F, 20);
		Arguments arguments2 = getPredictableRedGreenArguments(1F, 10F, 20);
		Arguments arguments3 = getPredictableRedGreenArguments(19F, 10F, 20);
		Arguments arguments4 = getPredictableRedGreenArguments(0F, 10F, 20);
		Arguments arguments5 = getPredictableGreenRedArguments(15F, 10F, 20);
		Arguments arguments6 = getPredictableGreenRedArguments(1F, 10F, 20);
		Arguments arguments7 = getPredictableGreenRedArguments(19F, 10F, 20);
		Arguments arguments8 = getPredictableGreenRedArguments(0F, 10F, 20);
		Arguments arguments9 = getPredictableGreenRedGreenArguments(15F, 10F, 5F, 20);
		Arguments arguments10 = getPredictableGreenRedGreenArguments(1F, 10F, 5F, 20);
		Arguments arguments11 = getPredictableGreenRedGreenArguments(19F, 10F, 5F, 20);
		Arguments arguments12 = getPredictableGreenRedGreenArguments(0F, 10F, 5F, 20);
		Arguments arguments13 = getPredictableGreenRedGreenArguments(15F, 1F, 15F, 20);
		Arguments arguments14 = getPredictableGreenRedGreenArguments(15F, 19F, 15F, 20);
		Arguments arguments15 = getPredictableGreenRedGreenArguments(15F, 0F, 15F, 20);
		Arguments arguments16 = getPredictableGreenRedGreenArguments(15F, 10F, 1F, 20);
		Arguments arguments17 = getPredictableGreenRedGreenArguments(15F, 10F, 4F, 20);
		Arguments arguments18 = getPredictableGreenRedGreenArguments(15F, 10F, 0F, 20);
		Arguments arguments19 = getPredictableGreenRedGreenArguments(10F, 10F, 10F, 20);
		return Stream.of(
				arguments0,
				arguments1,
				arguments2,
				arguments3,
				arguments4,
				arguments5,
				arguments6,
				arguments7,
				arguments8,
				arguments9,
				arguments10,
				arguments11,
				arguments12,
				arguments13,
				arguments14,
				arguments15,
				arguments16,
				arguments17,
				arguments18,
				arguments19);
	}

	private static Arguments getPredictableRedGreenArguments(
			Float timeLeft1, Float timeLeft2, int interval) {
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2),
				interval,
				timeLeft2 + (interval - timeLeft1),
				null,
				true,
				false);
	}

	private static Arguments getPredictableGreenRedArguments(
			Float timeLeft1, Float timeLeft2, int interval) {
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2),
				interval,
				null,
				timeLeft2 + (interval - timeLeft1),
				false,
				true);
	}

	private static Arguments getPredictableGreenRedGreenArguments(
			Float timeLeft1, Float timeLeft2, Float timeLeft3, int interval) {
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		TrafficApiCall trafficApiCall3 =
				TrafficApiCall.builder()
						.id(3L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft3)
						.executionNumber(3)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2, trafficApiCall3),
				interval,
				timeLeft3 + (interval - timeLeft2),
				timeLeft2 + (interval - timeLeft1),
				true,
				true);
	}

	@ParameterizedTest(name = "{2}s 간격으로 잔여 시간 {3}, {4} 데이터를 조회하면 예측이 불가능합니다")
	@MethodSource("unPredictableTraffics")
	void unPredictableCycle(
			List<TrafficApiCall> trafficApiCalls,
			boolean notValid,
			int interval,
			Float leftTime1,
			Float leftTime2) {
		// given
		PredictDatum predictDatum = new PredictDatum(traffic);
		PredictDatumPredictor predictDatumPredictor =
				new PredictDatumPredictor(predictDatum, trafficApiCalls, interval);

		// when
		Float greenCycle = predictDatumPredictor.predictGreenCycle();
		if (greenCycle != null) {
			predictDatum.loadGreenCycle(greenCycle);
		}
		Float redCycle = predictDatumPredictor.predictRedCycle();
		if (redCycle != null) {
			predictDatum.loadRedCycle(redCycle);
		}

		// then
		Assertions.assertNull(greenCycle);
		Assertions.assertNull(redCycle);
		log.debug(">>> greenCycle: " + greenCycle);
		log.debug(">>> redCycle: " + redCycle);
	}

	private static Stream<Arguments> unPredictableTraffics() {
		Arguments arguments1 = getUnPredictableGreenGreenArguments(35F, 10F, 20);
		Arguments arguments2 = getUnPredictableRedRedArguments(35F, 10F, 20);
		Arguments arguments3 = getNotValidArguments(15F, 5F, 5);
		return Stream.of(arguments1, arguments2, arguments3);
	}

	private static Arguments getUnPredictableGreenGreenArguments(
			Float timeLeft1, Float timeLeft2, int interval) {
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2), false, interval, timeLeft1, timeLeft2);
	}

	private static Arguments getUnPredictableRedRedArguments(
			Float timeLeft1, Float timeLeft2, int interval) {
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2), false, interval, timeLeft1, timeLeft2);
	}

	private static Arguments getNotValidArguments(Float timeLeft1, Float timeLeft2, int interval) {
		if (timeLeft1 < timeLeft2) {
			Float tmp = timeLeft1;
			timeLeft1 = timeLeft2;
			timeLeft2 = tmp;
		}
		TrafficApiCall trafficApiCall1 =
				TrafficApiCall.builder()
						.id(1L)
						.traffic(traffic)
						.color(TrafficColor.GREEN)
						.timeLeft(timeLeft1)
						.executionNumber(1)
						.build();
		TrafficApiCall trafficApiCall2 =
				TrafficApiCall.builder()
						.id(2L)
						.traffic(traffic)
						.color(TrafficColor.RED)
						.timeLeft(timeLeft2)
						.executionNumber(2)
						.build();
		return Arguments.of(
				Arrays.asList(trafficApiCall1, trafficApiCall2), true, interval, timeLeft1, timeLeft2);
	}
}
