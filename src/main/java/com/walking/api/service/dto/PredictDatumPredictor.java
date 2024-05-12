package com.walking.api.service.dto;

import com.walking.api.data.entity.TrafficApiCall;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PredictDatumPredictor {

	private final PredictDatum predictDatum;
	private final List<TrafficApiCall> data;
	private final int interval;

	public PredictDatumPredictor(PredictDatum predictDatum, List<TrafficApiCall> data, int interval) {
		this.predictDatum = predictDatum;
		this.data = data;
		this.interval = interval;
	}

	/**
	 * 예측이 끝나지 않았을 때 실행합니다.
	 *
	 * @param predicate predictData 가 예측이 끝나지 않았는지 확인하는 조건
	 * @param biFunction data 를 이용하여 predictData 의 cycle 값을 예측합니다.
	 * @return PredictDataPredictor 를 반환합니다.
	 */
	public PredictDatumPredictor ifNotPredictedApplyAndLoad(
			Predicate<PredictDatum> predicate,
			BiFunction<PredictDatumPredictor, List<TrafficApiCall>, Float> biFunction,
			BiConsumer<PredictDatum, Float> biConsumer) {
		if (!predicate.test(this.predictDatum)) {
			Float apply = biFunction.apply(this, this.data);
			biConsumer.accept(this.predictDatum, apply);
		}
		return this;
	}

	/**
	 * 예측을 종료합니다.
	 *
	 * @return PredictDatum 을 반환합니다.
	 */
	public PredictDatum done() {
		return this.predictDatum;
	}

	/**
	 * 빨간불의 사이클을 예측합니다.
	 *
	 * @param data 예측하고자 하는 신호등의 최근 데이터
	 * @return 인자로 전달 받은 predictData 에 예측 가능한 값을 채워 반환합니다.
	 */
	@Nullable
	public Float predictRedCycle(List<TrafficApiCall> data) {
		Optional<Float> optionalPredict = doPredict(data, isGreenPredicate(), isRedPredicate());
		return optionalPredict.orElse(null);
	}

	/**
	 * 내부 데이터를 이용하여 빨간불의 사이클을 예측합니다.<br>
	 * 만약 예측이 성공하면 predictData 에 값을 채워넣습니다.
	 *
	 * @return 예측한 사이클을 반환합니다.
	 */
	@Nullable
	public Float predictRedCycle() {
		return predictRedCycle(this.data);
	}

	/**
	 * 초록불의 사이클을 예측합니다.
	 *
	 * @param data 예측하고자 하는 신호등의 최근 데이터
	 * @return 인자로 전달 받은 predictData 에 예측 가능한 값을 채워 반환합니다.
	 */
	@Nullable
	public Float predictGreenCycle(List<TrafficApiCall> data) {
		Optional<Float> optionalPredict = doPredict(data, isRedPredicate(), isGreenPredicate());
		return optionalPredict.orElse(null);
	}

	/**
	 * 내부 데이터를 이용하여 초록불의 사이클을 예측합니다.<br>
	 * 만약 예측이 성공하면 predictDatum 에 값을 채워넣습니다.
	 *
	 * @return 예측한 사이클을 반환합니다.
	 */
	@Nullable
	public Float predictGreenCycle() {
		return predictGreenCycle(this.data);
	}

	private Predicate<TrafficApiCall> isRedPredicate() {
		return (TrafficApiCall tac) -> tac.getColor().isRed();
	}

	private Predicate<TrafficApiCall> isGreenPredicate() {
		return (TrafficApiCall tac) -> tac.getColor().isGreen();
	}

	private Optional<Float> doPredict(
			List<TrafficApiCall> data,
			Predicate<TrafficApiCall> beforeColorPredict,
			Predicate<TrafficApiCall> afterColorPredict) {
		Optional<Float> optionalCycle = Optional.empty();

		Iterator<TrafficApiCall> iterator = data.iterator();
		TrafficApiCall beforeData = iterator.next();

		while (iterator.hasNext()) {
			TrafficApiCall afterData = iterator.next();
			if (beforeColorPredict.test(beforeData) && afterColorPredict.test(afterData)) {
				// 시간을 계산한다.
				Float calculatedCycle = calculateCycle(afterData, beforeData);
				if (calculatedCycle < 0) {
					throw new IllegalArgumentException("예측이 불가능한 데이터입니다.");
				} else {
					optionalCycle = Optional.of(calculatedCycle);
					log.debug("패턴: " + beforeData.getColor() + " -> " + afterData.getColor() + " 을 찾았습니다.");
				}
				break;
			}
			beforeData = afterData;
		}
		return optionalCycle;
	}

	private Float calculateCycle(TrafficApiCall afterData, TrafficApiCall before) {
		return afterData.getTimeLeft() + this.interval - before.getTimeLeft();
	}
}
