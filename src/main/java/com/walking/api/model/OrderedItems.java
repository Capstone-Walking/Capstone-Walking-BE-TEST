package com.walking.api.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class OrderedItems implements Iterable<OrderedItem> {

	private final Long interval;

	private List<OrderedItem> items;

	public OrderedItems(List<? extends BasicItem> items, Long interval) {
		List<OrderedItem> itemSources = new ArrayList<>();
		long order = 0L;
		for (BasicItem item : items) {
			itemSources.add(new OrderedItem(item.getId(), item.getInterval(), order++));
		}
		this.items = itemSources;
		this.interval = interval;
	}

	@Override
	public Iterator<OrderedItem> iterator() {
		return items.iterator();
	}

	public OrderedItem getItemByOrder(Long order) {
		return items.stream()
				.filter(item -> item.getOrder().equals(order))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("해당 순서의 아이템이 없습니다."));
	}

	public OrderedItem getItemById(Long id) {
		return items.stream()
				.filter(item -> item.getId().equals(id))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("해당 아이템이 없습니다."));
	}

	public boolean isMatchOrder(OrderedItems requestItems) {
		for (int i = 0; i < items.size(); i++) {
			OrderedItem originItem = items.get(i);
			OrderedItem requestItem = requestItems.getItemById(originItem.getId());
			if (!requestItem.getOrder().equals(originItem.getOrder())) {
				return false;
			}
		}
		return true;
	}

	public OrderedItems changeOrder(Long originOrder, Long targetOrder) {
		/* 맨 뒤로 이동하는 경우 */
		if (targetOrder == items.size() - 1) {
			moveToEnd(originOrder, targetOrder);
			return this;
		}
		/* 맨 앞으로 이동하는 경우 */
		if (targetOrder == 0) {
			moveToFront(originOrder, targetOrder);
			return this;
		}

		long direction = targetOrder - originOrder;
		/* 뒤로 이동하는 경우 */
		if (direction > 0) {
			moveBack(originOrder, targetOrder);
			return this;
		}
		/* 앞으로 이동하는 경우 */
		if (direction < 0) {
			moveForward(originOrder, targetOrder);
			return this;
		}
		return this;
	}

	private void moveToEnd(Long origin, Long target) {
		Long originInterval = getItemByOrder(origin).getInterval();
		Long targetInterval = items.get(Math.toIntExact(target)).getInterval();
		Long updateInterval = targetInterval + interval;

		updateOrder(originInterval, updateInterval);
	}

	private void moveToFront(Long origin, Long target) {
		Long originInterval = getItemByOrder(origin).getInterval();
		Long targetInterval = items.get(Math.toIntExact(target)).getInterval();
		Long updateInterval = targetInterval / 2;

		updateOrder(originInterval, updateInterval);
	}

	private void moveBack(Long origin, Long target) {
		Long originInterval = getItemByOrder(origin).getInterval();
		Long targetInterval = items.get(Math.toIntExact(target)).getInterval();
		Long targetPlusOneInterval = items.get(Math.toIntExact(target + 1)).getInterval();
		Long updateInterval = (targetInterval + targetPlusOneInterval) / 2;

		/* interval이 같은 경우, 순서를 기준으로 interval을 재설정한다. */
		if (targetInterval.equals(updateInterval)) {
			List<OrderedItem> resetItems = new ArrayList<>();
			for (int i = 0; i < items.size(); i++) {
				resetItems.add(new OrderedItem(items.get(i).getId(), interval * i, (long) i));
			}
			OrderedItem o1 = resetItems.get(Math.toIntExact(origin));
			OrderedItem o2 = resetItems.get(Math.toIntExact(target));
			resetItems.set(Math.toIntExact(origin), o2);
			resetItems.set(Math.toIntExact(target), o1);
			for (int i = 0; i < resetItems.size(); i++) {
				resetItems.get(i).setOrder((long) i);
				resetItems.get(i).setInterval(interval * i);
			}
			items = resetItems;
			return;
		}

		updateOrder(originInterval, updateInterval);
	}

	private void moveForward(Long origin, Long target) {
		Long originInterval = getItemByOrder(origin).getInterval();
		Long targetInterval = items.get(Math.toIntExact(target)).getInterval();
		Long targetMinusOneInterval = items.get(Math.toIntExact(target - 1)).getInterval();
		Long updateInterval = (targetInterval + targetMinusOneInterval) / 2;

		updateOrder(originInterval, updateInterval);
	}

	private void updateOrder(Long originInterval, Long updateInterval) {
		items.stream()
				.filter(item -> item.getInterval().equals(originInterval))
				.findFirst()
				.ifPresent(item -> item.setInterval(updateInterval));

		items.sort((i1, i2) -> Math.toIntExact(i1.getInterval() - i2.getInterval()));
		for (int i = 0; i < items.size(); i++) {
			items.get(i).setOrder((long) i);
		}
	}
}
