package com.walking.api.service;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficApiCall;
import com.walking.api.repository.TrafficApiCallRepository;
import com.walking.api.service.constants.Interval;
import com.walking.api.service.dto.PredictData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 데이터베이스에 기록된 잔여시간 및 상태 정보를 바탕으로 신호 사이클을 예측합니다. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrafficCyclePredictService {

	private final TrafficApiCallRepository trafficApiCallRepository;

	/**
	 * 최신 순으로 interval 개 만큼 씩 데이터를 가지고 와서 계산을 수행 예를 들어, interval이 5인 경우 한 번 예측을 시도할 때마다 5개의 데이터를 가져와
	 * 예측을 수행합니다.
	 *
	 * @param traffics 신호 주기를 예측하고자 하는 신호등 리스트
	 * @param interval 예측을 위해 가져올 데이터의 크기
	 */
	// todo: 예외 처리
	public Map<Traffic, PredictData> execute(List<Traffic> traffics, int interval) {
		int start = 0;
		int end = start + interval;

		// 예측 정보를 담고 반환될 변수(리턴값)
		Map<Traffic, PredictData> result = new HashMap<>();
		for (Traffic traffic : traffics) {
			result.put(traffic, new PredictData(traffic));
		}

		// 예측이 끝나지 않은 신호등 리스트
		List<Traffic> unpredictedList = getUnpredictedList(result);

		while (!unpredictedList.isEmpty()) {
			List<TrafficApiCall> recentlyData =
					trafficApiCallRepository.getRecentlyData(unpredictedList, start, end);

			Map<Traffic, List<TrafficApiCall>> separatedData = separateByTraffic(recentlyData);
			logging(separatedData);

			// 여기서 예측이 불가능한 신호등인지 구분되고 루프가 종료됩니다.
			if (separatedData.isEmpty()) {
				break;
			}

			for (Traffic traffic : separatedData.keySet()) {
				predict(separatedData.get(traffic), result.get(traffic));
			}

			unpredictedList = getUnpredictedList(result);
			start = end;
			end += interval;
		}

		return result;
	}

	/**
	 * 가져온 신호등 정보를 신호등을 기준으로 데이터를 분리합니다.
	 *
	 * @param recentlyData 데이터
	 * @return 신호등을 key 로 갖는 Map
	 */
	private Map<Traffic, List<TrafficApiCall>> separateByTraffic(List<TrafficApiCall> recentlyData) {
		Map<Traffic, List<TrafficApiCall>> separatedData = new HashMap<>();

		for (TrafficApiCall recentlyDatum : recentlyData) {
			List<TrafficApiCall> group =
					separatedData.computeIfAbsent(recentlyDatum.getTraffic(), data -> new ArrayList<>());

			group.add(recentlyDatum);
		}

		return separatedData;
	}

	/**
	 * 예측을 수행한 결과를 읽어보고 예측이 아직 끝나지 않은 신호등 리스트를 반환합니다.
	 *
	 * @param result 예측을 수행한 결과
	 * @return 신호등 리스트
	 */
	private List<Traffic> getUnpredictedList(Map<Traffic, PredictData> result) {
		List<Traffic> unpredictedList = new ArrayList<>();
		for (Traffic traffic : result.keySet()) {
			if (!result.get(traffic).isComplete()) {
				unpredictedList.add(traffic);
			}
		}

		return unpredictedList;
	}

	/**
	 * 최근 데이터와 예측하고 있는 정보를 가지고 신호등 사이클을 계산합니다.
	 *
	 * @param data 예측하고자 하는 신호등의 최근 데이터
	 * @param predictData 예측된 정보
	 * @return 인자로 전달 받은 predictData 에 예측 가능한 값을 채워 반환합니다.
	 */
	// R -> G, G -> R 따로 찾지말고 한 번 순회할 때 모두 찾아내면 좋겠다
	private PredictData predict(List<TrafficApiCall> data, PredictData predictData) {
		if (!predictData.isPredictedGreenCycle()) {
			predictData.updateGreenCycle(getGreenCycle(data));
		}
		if (!predictData.isPredictedRedCycle()) {
			predictData.updateRedCycle(getRedCycle(data));
		}

		return predictData;
	}

	// todo: getRedCycle 과 getGreenCycle 메서드 하나로 합치기

	/**
	 * 신호등의 빨간불에 대해서 사이클을 계산합니다.
	 *
	 * @param data 계산하고자 하는 신호등의 데이터 리스트
	 * @return 빨간불의 사이클
	 */
	private static Optional<Float> getRedCycle(List<TrafficApiCall> data) {
		Optional<Float> redCycle = Optional.empty();

		Iterator<TrafficApiCall> iterator = data.iterator();
		TrafficApiCall afterData = iterator.next();

		while (iterator.hasNext()) {
			TrafficApiCall before = iterator.next();
			// G -> R 인 패턴을 찾는다.
			if (before.getColor().isGreen() && afterData.getColor().isRed()) {
				// 시간을 계산한다.
				redCycle =
						Optional.of(
								afterData.getTimeLeft() + Interval.SCHEDULER_INTERVAL - before.getTimeLeft());
				log.debug("패턴: " + before.getColor() + " -> " + afterData.getColor() + " 을 찾았습니다.");
				break;
			}
			afterData = before;
		}
		return redCycle;
	}

	/**
	 * 신호등의 초록불에 대해서 사이클을 계산합니다.
	 *
	 * @param data 계산하고자 하는 신호등의 데이터 리스트
	 * @return 초록불의 사이클
	 */
	private static Optional<Float> getGreenCycle(List<TrafficApiCall> data) {
		Optional<Float> greenCycle = Optional.empty();

		Iterator<TrafficApiCall> iterator = data.iterator();
		TrafficApiCall afterData = iterator.next();

		while (iterator.hasNext()) {
			TrafficApiCall before = iterator.next();
			// R -> G 인 패턴을 찾는다.
			if (before.getColor().isRed() && afterData.getColor().isGreen()) {
				// 시간을 계산한다.
				greenCycle =
						Optional.of(
								afterData.getTimeLeft() + Interval.SCHEDULER_INTERVAL - before.getTimeLeft());
				log.debug("패턴: " + before.getColor() + " -> " + afterData.getColor() + " 을 찾았습니다.");
				break;
			}
			afterData = before;
		}
		return greenCycle;
	}

	/**
	 * 가져온 데이터가 무엇인지 로그로 출력합니다.
	 *
	 * @param separatedData 신호등 단위로 구분된 데이터 리스트
	 */
	private void logging(Map<Traffic, List<TrafficApiCall>> separatedData) {
		log.debug("==================== 가져온 데이터 ====================");
		for (Traffic traffic : separatedData.keySet()) {
			log.debug("key == " + traffic.getId());
			List<TrafficApiCall> data = separatedData.get(traffic);
			for (TrafficApiCall datum : data) {
				log.debug(datum.toString());
			}
		}
		log.debug("==================== 가져온 데이터 끝 ====================");
	}
}
