package com.walking.api.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 신호등이 가지고 있는 고유한 신호 사이클 */
@Entity
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TrafficCycle {

	@Id @GeneratedValue private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	private Traffic traffic;

	@Column(nullable = false)
	private Double redDuration;

	@Column(nullable = false)
	private Double greenDuration;
}
