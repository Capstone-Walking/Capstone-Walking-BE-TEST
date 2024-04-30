package com.walking.api.service;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.FavoriteItemWithInterval;
import com.walking.api.data.entity.Member;
import com.walking.api.service.dto.ItemAndOrder;
import com.walking.api.service.dto.ModifyOrderIntervalDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifyOrderService {

	private final EntityManager em;

	/** workTable 변경되고 있는 정보를 저장 key: 정렬 순서, value: 정렬될 아이템과 실제 순서 값 */
	private List<FavoriteItemWithInterval> workTable = new ArrayList<>();

	private int interval;

	/**
	 * category 에 소속된 item 들의 순서를 변경합니다. item 의 순서가 변경되는 과정은 변경 후에 가장 앞으로 와야하는 원소 순으로 변경이 진행됩니다.
	 *
	 * @param request 순서가 바뀌고 난 후의 기대되는 category 내의 item 리스트
	 */
	@Transactional
	public void execute(ModifyOrderIntervalDto request) {
		Category category = request.getCategory();
		Member member = request.getMember();
		List<FavoriteItemWithInterval> beforeList =
				new ArrayList<>(
						em.createQuery(
										"select fiwi from FavoriteItemWithInterval fiwi "
												+ " where fiwi.member = :member "
												+ "     and fiwi.category = :category "
												+ " order by fiwi.order",
										FavoriteItemWithInterval.class)
								.setParameter("member", member)
								.setParameter("category", category)
								.getResultList());

		initWorkTable(beforeList);

		interval = category.getOrderInterval();

		ItemAndOrder[] requestedData = request.getItemAndOrders();
		inputSizeValidation(requestedData, beforeList);
		Arrays.sort(requestedData);

		// 데이터의 순서를 변경
		// requestData & beforeList는 Order 기준 오름차순 정렬된 데이터
		// 같은 인덱스에 같은 아이템 -> 변경을 요청하지 않음
		// 같은 인덱스에 다른 아이템 -> 변경 대상
		for (int i = 0; i < beforeList.size(); i++) {
			// 순서가 바뀌어야 하는 데이터인 경우
			if (requestedData[i].getItem() != beforeList.get(i).getItem()) {

				int afterOrder = requestedData[i].getOrder();

				// todo: 이전 원소의 순서를 찾는 과정 개선
				FavoriteItemWithInterval singleResult =
						em.createQuery(
										"select fiwi from FavoriteItemWithInterval fiwi "
												+ " where fiwi.category = :category "
												+ " and fiwi.member = :member"
												+ " and fiwi.item = :item",
										FavoriteItemWithInterval.class)
								.setParameter("category", category)
								.setParameter("member", member)
								.setParameter("item", requestedData[i].getItem())
								.getSingleResult();

				int beforeOrder = workTable.indexOf(singleResult);

				log.info(
						requestedData[i].getItem() + "을 " + beforeOrder + " -> " + afterOrder + "로 변경합니다.");
				changeOrder(beforeOrder, afterOrder);
			}
		}
	}

	private void inputSizeValidation(
			ItemAndOrder[] requestedData, List<FavoriteItemWithInterval> beforeList) {
		if (requestedData.length != beforeList.size()) {
			throw new IllegalArgumentException("전체 즐겨찾기 데이터를 전달해야 합니다.");
		}
	}

	private void initWorkTable(List<FavoriteItemWithInterval> beforeList) {
		workTable = new ArrayList<>(beforeList.stream().collect(Collectors.toList()));
	}

	// todo: 각각의 변경 메서드에 대하여 interval 을 수정할지 검증하는 절차가 필요
	private void changeOrder(int beforeOrder, int afterOrder) {
		int direction = afterOrder - beforeOrder;

		if (afterOrder == workTable.size() - 1) { // 맨 뒤로 이동하는 경우
			moveToEnd(beforeOrder, afterOrder);
		} else if (afterOrder == 0) { // 맨 앞으로 이동하는 경우
			moveToFront(beforeOrder, afterOrder);
		} else if (direction > 0) { // 뒤로 이동하는 경우
			moveBack(beforeOrder, afterOrder);
		} else if (direction < 0) { // 앞으로 이동하는 경우
			moveForward(beforeOrder, afterOrder);
		} else {
			return;
		}
	}

	private void moveToEnd(int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int resultOrderValue = afterOrderValue + interval;

		updateOrder(beforeOrder, resultOrderValue);
	}

	private void moveToFront(int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int resultOrderValue = afterOrderValue / 2;

		updateOrder(beforeOrder, resultOrderValue);
	}

	private void moveBack(int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int afterAfterOrderValue = workTable.get(afterOrder + 1).getOrder();
		int resultOrderValue = (afterOrderValue + afterAfterOrderValue) / 2;

		updateOrder(beforeOrder, resultOrderValue);
	}

	private void moveForward(int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int beforeAfterOrderValue = workTable.get(afterOrder - 1).getOrder();
		int resultOrderValue = (afterOrderValue + beforeAfterOrderValue) / 2;

		updateOrder(beforeOrder, resultOrderValue);
	}

	private void updateOrder(int beforeOrder, int resultOrderValue) {
		workTable.get(beforeOrder).setOrder(resultOrderValue);
		workTable.sort(
				(i1, i2) -> {
					return i1.getOrder() - i2.getOrder();
				});
	}
}
