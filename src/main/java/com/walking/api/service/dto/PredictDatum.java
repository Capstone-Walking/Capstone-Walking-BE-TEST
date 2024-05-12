package com.walking.api.service.dto;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficApiCall;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/** 예측한 결과를 나타내는 클래스 */
@Slf4j
@Getter
@AllArgsConstructor
@ToString
public class PredictDatum {

	private Traffic traffic;
	@Nullable private Float redCycle;
	@Nullable private Float greenCycle;

	public PredictDatum(Traffic traffic) {
		this.traffic = traffic;
	}

	/**
	 * 빨간불의 사이클을 갱신합니다.
	 *
	 * @param redCycle 빨간불의 사이클
	 * @return PredictDatum 객체를 반환합니다.
	 */
	public PredictDatum loadRedCycle(Float redCycle) {
		this.redCycle = redCycle;
		return this;
	}

	/**
	 * 초록불의 사이클을 갱신합니다.
	 *
	 * @param greenCycle 초록불의 사이클
	 * @return PredictDatum 객체를 반환합니다.
	 */
	public PredictDatum loadGreenCycle(Float greenCycle) {
		this.greenCycle = greenCycle;
		return this;
	}

	public Optional<Float> getRedCycle() {
		return Optional.ofNullable(redCycle);
	}

	public Optional<Float> getGreenCycle() {
		return Optional.ofNullable(greenCycle);
	}

	/**
	 * 빨간불의 사이클이 계산 되었는지 반환합니다.
	 *
	 * @return redCycle 이 존재하면 true
	 */
	public boolean isPredictedRedCycle() {
		return Objects.nonNull(redCycle);
	}

	/**
	 * 초록불의 사이클이 계산 되었는지 반환합니다.
	 *
	 * @return greenCycle 이 존재하면 true
	 */
	public boolean isPredictedGreenCycle() {
		return Objects.nonNull(greenCycle);
	}

	/**
	 * 예측이 끝났는지 여부를 반환합니다.
	 *
	 * @return 예측이 끝났으면 true, 아니면 false
	 */
	public boolean isComplete() {
		return this.isPredictedRedCycle() && this.isPredictedGreenCycle();
	}

	/**
	 * PredictDataPredictor 객체를 통해 예측을 수행합니다.
	 *
	 * @param data 예측하고자 하는 신호등의 최근 데이터
	 * @return PredictDataPredictor 객체를 반환합니다.
	 */
	public PredictDatumPredictor predict(List<TrafficApiCall> data, int interval) {
		return new PredictDatumPredictor(this, data, interval);
	}
}
