package com.walking.api.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FavoriteItemWithInterval {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	private Item item;

	// todo: request에서 사용하는 order와 변수명 구분하기
	@Column(name = "orders", nullable = false)
	private Integer order;

	@Override
	public String toString() {
		return "FavoriteItemWithInterval{"
				+ "id="
				+ id
				+ ", member="
				+ member
				+ ", category="
				+ category
				+ ", item="
				+ item
				+ ", order="
				+ order
				+ '}';
	}
}
