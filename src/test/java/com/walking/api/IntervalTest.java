package com.walking.api;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.FavoriteItemWithInterval;
import com.walking.api.data.entity.Item;
import com.walking.api.data.entity.Member;
import com.walking.api.service.ModifyOrderService;
import com.walking.api.service.dto.ItemAndOrder;
import com.walking.api.service.dto.ModifyOrderIntervalDto;
import com.walking.api.utils.TestDataGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class IntervalTest extends RepositoryTest {

	@Nested
	class LONG_INTERVAL_TEST {

		ModifyOrderService modifyOrderService = new ModifyOrderService(em);
		TestDataGenerator testDataGenerator = new TestDataGenerator(em);

		@ParameterizedTest(name = "간격이 {0}인 데이터를 {1}개 생성 후 {1}개의 순서 수정")
		@CsvSource("512, 4")
		void simpleExample(int interval, int count) {

			// 테스트에 필요한 데이터를 준비합니다.
			Member member =
					em.createQuery("select m from Member m where m.id = 1L", Member.class).getSingleResult();
			Category category =
					em.createQuery(
									"select c from Category c "
											+ " where c.member = :member "
											+ " and c.orderInterval = :interval",
									Category.class)
							.setParameter("member", member)
							.setParameter("interval", interval)
							.getSingleResult();

			testDataGenerator.executeWithInterval(category, interval, count);

			log.info("순서 변경 요청 데이터 생성 중...");
			ItemAndOrder[] itemAndOrders = new ItemAndOrder[count];
			List<Item> items =
					new ArrayList<>(
							em.createQuery("select i from Item i where i.id <= 4L order by i.id", Item.class)
									.getResultList());

			// 1,2,3,4 -> 4,1,3,2 로 순서 변경 데이터 생성
			itemAndOrders[0] = ItemAndOrder.builder().item(items.get(0)).order(1).build();
			itemAndOrders[1] = ItemAndOrder.builder().item(items.get(1)).order(3).build();
			itemAndOrders[2] = ItemAndOrder.builder().item(items.get(2)).order(2).build();
			itemAndOrders[3] = ItemAndOrder.builder().item(items.get(3)).order(0).build();

			ModifyOrderIntervalDto request =
					ModifyOrderIntervalDto.builder()
							.member(member)
							.category(category)
							.itemAndOrders(itemAndOrders)
							.build();

			modifyOrderService.execute(request);

			log.info("변경 후 결과값");
			List<FavoriteItemWithInterval> resultList =
					new ArrayList<>(
							em.createQuery(
											"select fiwi from FavoriteItemWithInterval fiwi "
													+ " where fiwi.member = :member and fiwi.category = :category"
													+ " order by fiwi.order",
											FavoriteItemWithInterval.class)
									.setParameter("member", member)
									.setParameter("category", category)
									.getResultList());

			for (FavoriteItemWithInterval result : resultList) {
				log.info(result.toString());
			}

			Assertions.assertThat(resultList.get(0).getItem()).isEqualTo(items.get(3));
			Assertions.assertThat(resultList.get(1).getItem()).isEqualTo(items.get(0));
			Assertions.assertThat(resultList.get(2).getItem()).isEqualTo(items.get(2));
			Assertions.assertThat(resultList.get(3).getItem()).isEqualTo(items.get(1));
		}
	}

	@Nested
	class SHORT_INTERVAL_TEST {}
}
