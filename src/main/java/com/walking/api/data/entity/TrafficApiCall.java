package com.walking.api.data.entity;

import com.walking.api.data.constant.TrafficColor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

/** 신호제어기 잔여시간 정보 서비스의 응답 결과 */
@Entity
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@BatchSize(size = 1000)
public class TrafficApiCall {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 신호등
	@ManyToOne(fetch = FetchType.LAZY)
	private Traffic traffic;

	// 신호등 색상
	@Enumerated(value = EnumType.STRING)
	@Column(nullable = false)
	private TrafficColor color;

	// 잔여시간
	@Column(nullable = false)
	private Float timeLeft;

	// executionNumber 가 클수록 최근에 API call 을 통해 받아온 데이터
	// 실제 프로젝트에는 JobExecutionId로 대체할 부분
	@Column(nullable = false)
	private Integer executionNumber;
}
