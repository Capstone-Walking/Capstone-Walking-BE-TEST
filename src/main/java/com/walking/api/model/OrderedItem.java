package com.walking.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderedItem extends BasicItem {

	private Long order;

	public OrderedItem(Long id, Long interval, Long order) {
		super(id, interval);
		this.order = order;
	}
}
