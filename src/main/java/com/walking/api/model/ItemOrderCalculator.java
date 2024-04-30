package com.walking.api.model;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemOrderCalculator {

	public OrderedItems execute(OrderedItems compareItems, OrderedItems requestItems) {
		OrderedItems originItems =
				new OrderedItems(compareItems.getItems(), compareItems.getInterval());

		int flag = 0;
		for (int i = 0; i < originItems.getItems().size(); i++) {
			OrderedItem originItem = originItems.getItemByOrder((long) i);
			OrderedItem requestItem = requestItems.getItemById(originItem.getId());
			if (!requestItem.getOrder().equals(originItem.getOrder())) {
				Long requestOrder = requestItem.getOrder();
				Long originOrder = compareItems.getItemById(originItem.getId()).getOrder();
				compareItems = compareItems.changeOrder(originOrder, requestOrder);
				flag++;
			}
		}

		/* 모든 아이템이 같은 순서일 경우, 요청된 순서를 그대로 반환한다. */
		if (flag == originItems.getItems().size()) {
			List<OrderedItem> items = requestItems.getItems();
			List<OrderedItem> orderedItems = new ArrayList<>();
			long idx = 1;
			Long interval = requestItems.getInterval();
			for (OrderedItem item : items) {
				orderedItems.add(new OrderedItem(item.getId(), interval * idx, idx++));
			}
			log.info("*****************************************************************");
			log.info("all items are changed");
			log.info("*****************************************************************");
			return new OrderedItems(orderedItems, interval);
		}

		if (!compareItems.isMatchOrder(requestItems)) {
			log.info("#################################################################");
			log.info("items are not matched");
			log.info("#################################################################");
			return execute(compareItems, requestItems);
		}

		log.info("#################################################################");
		log.info("items are changed");
		log.info("#################################################################");
		return compareItems;
	}
}
