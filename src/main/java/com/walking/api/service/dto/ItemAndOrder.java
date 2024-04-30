package com.walking.api.service.dto;

import com.walking.api.data.entity.Item;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ItemAndOrder implements Comparable<ItemAndOrder> {

	private Item item;
	private Integer order;

	@Override
	public int compareTo(ItemAndOrder o) {
		return this.getOrder() - o.getOrder();
	}
}
