package com.walking.api.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "favorite_item_with_interval")
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
