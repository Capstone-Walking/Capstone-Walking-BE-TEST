package com.walking.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class BasicItemsTest {

	ItemOrderCalculator itemOrderCalculator = new ItemOrderCalculator();

	static class TestItem extends BasicItem {
		public TestItem(Long id, Long interval) {
			super(id, interval);
		}
	}

	static class TestItems extends OrderedItems {
		public TestItems(List<? extends BasicItem> items, Long interval) {
			super(items, interval);
		}
	}

	@ParameterizedTest(name = "변경 결과: {0}")
	@MethodSource("orders")
	void 순서변경을_테스트합니다(List<Integer> orders) {
		// Given
		long interval = 1024L;
		List<BasicItem> testItemSources = new ArrayList<>();
		for (int i = 1; i <= orders.size(); i++) {
			testItemSources.add(new TestItem((long) i, interval * i));
		}
		OrderedItems compareItems = new TestItems(testItemSources, interval);

		List<BasicItem> requestItemSources = new ArrayList<>();
		orders.forEach(
				order -> {
					requestItemSources.add(new TestItem((long) order, interval * order));
				});
		OrderedItems requestItems = new TestItems(requestItemSources, interval);

		// When
		OrderedItems reorderedItems = itemOrderCalculator.execute(compareItems, requestItems);

		// Then
		log.info("{}", reorderedItems);
		for (int i = 0; i < orders.size(); i++) {
			List<OrderedItem> items = reorderedItems.getItems();
			OrderedItem orderedItem = items.get(i);
			log.info("{}", orderedItem);
			Assertions.assertEquals(Long.valueOf(orders.get(i)), orderedItem.getId());
		}
	}

	static List<List<Integer>> orders() {
		List<Integer> elements = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
		return generatePermutations(elements);
	}

	public static List<List<Integer>> generatePermutations(List<Integer> elements) {
		List<List<Integer>> permutations = new ArrayList<>();
		generatePermutationsHelper(elements, new ArrayList<>(), permutations);
		return permutations;
	}

	private static void generatePermutationsHelper(
			List<Integer> elements, List<Integer> permutation, List<List<Integer>> permutations) {
		if (elements.isEmpty()) {
			permutations.add(new ArrayList<>(permutation));
			return;
		}

		for (int i = 0; i < elements.size(); i++) {
			int currentElement = elements.get(i);
			permutation.add(currentElement);
			List<Integer> remainingElements = new ArrayList<>(elements);
			remainingElements.remove(i);
			generatePermutationsHelper(remainingElements, permutation, permutations);
			permutation.remove(permutation.size() - 1);
		}
	}
}
