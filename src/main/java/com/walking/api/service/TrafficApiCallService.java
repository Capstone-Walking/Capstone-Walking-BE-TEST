package com.walking.api.service;

import com.walking.api.data.constant.TrafficColor;
import com.walking.api.data.entity.TrafficApiCall;
import com.walking.api.data.entity.TrafficCycle;
import com.walking.api.repository.TrafficApiCallRepository;
import com.walking.api.repository.TrafficCycleRepository;
import com.walking.api.service.dto.ColorAndTimeLeft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신호 시간 및 색상 정보를 받아온 척 데이터를 만들어 냅니다. (api 요청을 1회 보낸 것과 동일) */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrafficApiCallService {

	private final TrafficApiCallRepository trafficApiCallRepository;
	private final TrafficCycleRepository trafficCycleRepository;

	/**
	 * 가장 최근에 받아온 TrafficApiCall 데이터와 TrafficCycle, apiCallInterval 을 이용하여 데이터를 만들어냅니다.
	 *
	 * @param apiCallInterval api 데이터를 받아오는 스케줄러의 주기[단위 (초)]
	 * @param executionNumber JobExecution 을 나타내는 변수, 몇 번째로 생성된 데이터 인지를 나타냅니다
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<TrafficApiCall> execute(int apiCallInterval, int executionNumber) {
		TrafficApiCall[] recentlyData =
				trafficApiCallRepository
						.findByExecutionNumber(executionNumber - 1)
						.toArray(new TrafficApiCall[] {});
		TrafficCycle[] trafficCycles = trafficCycleRepository.findAll().toArray(new TrafficCycle[] {});

		// 쉬운 비교를 위해 두 데이터를 신호등 아이디로 정렬
		Arrays.sort(
				recentlyData,
				(t1, t2) -> {
					return Long.compare(t1.getTraffic().getId(), t2.getTraffic().getId());
				});
		Arrays.sort(
				trafficCycles,
				(t1, t2) -> {
					return Long.compare(t1.getTraffic().getId(), t2.getTraffic().getId());
				});

		List<TrafficApiCall> nextApiCallData =
				generateNextApiCallData(apiCallInterval, executionNumber, recentlyData, trafficCycles);

		return trafficApiCallRepository.saveAll(nextApiCallData);
	}

	/**
	 * 가장 최근 API 응답 데이터와 신호 주기를 통해 계산한 다음 API 응답 데이터를 생성합니다.
	 *
	 * @param apiCallInterval 스케줄러의 주기
	 * @param recentlyData 가장 최근에 응답 받은 신호 잔여시간 데이터
	 * @param trafficCycles 각 신호등에 대한 주기 데이터
	 * @return 각각의 신호등 별로 생성된 다음 API 응답 데이터
	 */
	private List<TrafficApiCall> generateNextApiCallData(
			int apiCallInterval,
			int executionNumber,
			TrafficApiCall[] recentlyData,
			TrafficCycle[] trafficCycles) {

		List<TrafficApiCall> nextApiCallData = new ArrayList<>();
		ColorAndTimeLeft nextColorAndTimeLeft;

		for (int i = 0; i < recentlyData.length; i++) {
			validateSameData(recentlyData[i], trafficCycles[i]);

			nextColorAndTimeLeft =
					generateNextColorAndTimeLeft(apiCallInterval, recentlyData[i], trafficCycles[i]);
			TrafficApiCall callData =
					TrafficApiCall.builder()
							.traffic(recentlyData[i].getTraffic())
							.color(nextColorAndTimeLeft.getTrafficColor())
							.timeLeft(nextColorAndTimeLeft.getTimeLeft())
							.executionNumber(executionNumber)
							.build();
			nextApiCallData.add(callData);
			log.debug("[" + i + "] call data : " + callData.toString());
		}

		return nextApiCallData;
	}

	/**
	 * 계산에 필요한 최근 데이터와 신호등의 사이클이 같은 신호등에 대한 정보인지 검증합니다.
	 *
	 * @param recentlyData 계산하고자 하는 신호등의 최근 api 응답 데이터
	 * @param trafficCycles 계산하고자 하는 신호등의 주기
	 */
	private static void validateSameData(TrafficApiCall recentlyData, TrafficCycle trafficCycles) {
		if (recentlyData.getTraffic().getId() != trafficCycles.getTraffic().getId()) {
			throw new IllegalStateException("자료 불일치");
		}
	}

	/**
	 * 다음 신호등의 색상과 잔여시간을 계산하여 반환합니다.
	 *
	 * @param apiCallInterval 설정된 스케줄러의 주기
	 * @param recentlyDatum 특정 신호등에 대해 가장 최근에 받아온 api 응답 데이터
	 * @param trafficCycle 특정 신호등의 주기
	 * @return 다음 신호등의 색상 및 잔여시간
	 */
	private ColorAndTimeLeft generateNextColorAndTimeLeft(
			int apiCallInterval, TrafficApiCall recentlyDatum, TrafficCycle trafficCycle) {
		float timeLeftOfNextTrafficColor = apiCallInterval - recentlyDatum.getTimeLeft();
		TrafficColor nextTrafficColor = recentlyDatum.getColor();

		while (timeLeftOfNextTrafficColor >= 0) {
			nextTrafficColor = nextTrafficColor.getNextColor();
			if (nextTrafficColor.isRed()) {
				timeLeftOfNextTrafficColor -= trafficCycle.getRedCycle();
			} else {
				timeLeftOfNextTrafficColor -= trafficCycle.getGreenCycle();
			}
		}

		// 소수점 둘째 자리에서 반올림
		timeLeftOfNextTrafficColor = (float) (Math.round(timeLeftOfNextTrafficColor * -10) / 10.0);

		return new ColorAndTimeLeft(nextTrafficColor, timeLeftOfNextTrafficColor);
	}
}
