package com.walking.api.service;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.FavoriteItemWithInterval;
import com.walking.api.data.entity.Item;
import com.walking.api.data.entity.Member;
import com.walking.api.service.dto.ItemAndOrder;
import com.walking.api.service.dto.ModifyOrderIntervalDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	/**
	 * category 에 소속된 item 들의 순서를 변경합니다. item 의 순서가 변경되는 과정은 변경 후에 가장 앞으로 와야하는 원소 순으로 변경이 진행됩니다.
	 *
	 * @param request 순서가 바뀌고 난 후의 기대되는 category 내의 item 리스트
	 */
	@Transactional
	public void execute(ModifyOrderIntervalDto request) {
		Category category = request.getCategory();
		Member member = request.getMember();
		ItemAndOrder[] requestData = request.getItemAndOrders();
		List<FavoriteItemWithInterval> originData =
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
		inputSizeValidation(requestData, originData);

		List<FavoriteItemWithInterval> workTable = new ArrayList<>(originData);
		Arrays.sort(requestData);

		/*
		데이터의 순서를 변경
		requestData & originData는 Order 기준 오름차순 정렬된 데이터
		같은 인덱스에 같은 아이템 -> 변경을 요청하지 않음
		같은 인덱스에 다른 아이템 -> 변경 대상
		*/
		for (int i = 0; i < originData.size(); i++) {
			// 순서가 바뀌어야 하는 데이터인 경우
			if (requestData[i].getItem() != originData.get(i).getItem()) {
				int afterOrder = requestData[i].getOrder();
				Item item = requestData[i].getItem();
				FavoriteItemWithInterval singleResult =
						originData.stream()
								.filter(d -> d.getItem().equals(item))
								.findFirst()
								.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이템"));
				int beforeOrder = workTable.indexOf(singleResult);

				log.info(requestData[i].getItem() + "을 " + beforeOrder + " -> " + afterOrder + "로 변경합니다.");
				changeOrder(workTable, beforeOrder, afterOrder, category.getOrderInterval());
			}
		}
	}

	private void inputSizeValidation(
			ItemAndOrder[] requestedData, List<FavoriteItemWithInterval> beforeList) {
		if (requestedData.length != beforeList.size()) {
			throw new IllegalArgumentException("전체 즐겨찾기 데이터를 전달해야 합니다.");
		}
	}

	// todo: 각각의 변경 메서드에 대하여 interval 을 수정할지 검증하는 절차가 필요
	private List<FavoriteItemWithInterval> changeOrder(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int afterOrder, int interval) {
		int direction = afterOrder - beforeOrder;
		if (afterOrder == workTable.size() - 1) { // 맨 뒤로 이동하는 경우
			return moveToEnd(workTable, beforeOrder, afterOrder, interval);
		}
		if (afterOrder == 0) { // 맨 앞으로 이동하는 경우
			return moveToFront(workTable, beforeOrder, afterOrder);
		}
		if (direction > 0) { // 뒤로 이동하는 경우
			return moveBack(workTable, beforeOrder, afterOrder);
		}
		if (direction < 0) { // 앞으로 이동하는 경우
			return moveForward(workTable, beforeOrder, afterOrder);
		}
		return workTable;
	}

	private List<FavoriteItemWithInterval> moveToEnd(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int afterOrder, int interval) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int resultOrderValue = afterOrderValue + interval;

		return updateOrder(workTable, beforeOrder, resultOrderValue);
	}

	private List<FavoriteItemWithInterval> moveToFront(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int resultOrderValue = afterOrderValue / 2;

		return updateOrder(workTable, beforeOrder, resultOrderValue);
	}

	private List<FavoriteItemWithInterval> moveBack(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int afterAfterOrderValue = workTable.get(afterOrder + 1).getOrder();
		int resultOrderValue = (afterOrderValue + afterAfterOrderValue) / 2;

		return updateOrder(workTable, beforeOrder, resultOrderValue);
	}

	private List<FavoriteItemWithInterval> moveForward(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int afterOrder) {
		int afterOrderValue = workTable.get(afterOrder).getOrder();
		int beforeAfterOrderValue = workTable.get(afterOrder - 1).getOrder();
		int resultOrderValue = (afterOrderValue + beforeAfterOrderValue) / 2;

		return updateOrder(workTable, beforeOrder, resultOrderValue);
	}

	private List<FavoriteItemWithInterval> updateOrder(
			List<FavoriteItemWithInterval> workTable, int beforeOrder, int resultOrderValue) {
		workTable.get(beforeOrder).setOrder(resultOrderValue);
		workTable.sort(
				(i1, i2) -> {
					return i1.getOrder() - i2.getOrder();
				});
		return workTable;
	}
}
