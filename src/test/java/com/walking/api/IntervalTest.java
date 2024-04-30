package com.walking.api;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.FavoriteItemWithInterval;
import com.walking.api.data.entity.Item;
import com.walking.api.data.entity.Member;
import com.walking.api.service.ModifyOrderService;
import com.walking.api.service.dto.ItemAndOrder;
import com.walking.api.service.dto.ModifyOrderIntervalDto;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Slf4j
@ContextConfiguration(classes = {ModifyOrderService.class})
class IntervalTest extends RepositoryTest {

	@Autowired ModifyOrderService modifyOrderService;

	@Nested
	class LONG_INTERVAL_TEST {

		@ParameterizedTest(name = "간격이 {0}인 데이터를 {1}개 생성 후 {1}개의 순서 수정")
		@CsvSource("512, 4")
		void simpleExample(int interval, int count) {
			// Given
			Member member = getMember(1L);
			Category category = getCategory(interval, member);
			List<Item> items = getItems(4L);
			setFavoriteItemsWithInterval(items, interval, count, member, category);

			// 1,2,3,4 -> 4,1,3,2 로 순서 변경 데이터 생성
			ItemAndOrder[] itemAndOrders = new ItemAndOrder[count];
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

			// When
			log.info("===========================================================");
			modifyOrderService.execute(request);

			// Then
			log.info("===========================================================");
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

			log.info("===========================================================");
			for (FavoriteItemWithInterval result : resultList) {
				log.info(result.toString());
			}

			Assertions.assertThat(resultList.get(0).getItem()).isEqualTo(items.get(3));
			Assertions.assertThat(resultList.get(1).getItem()).isEqualTo(items.get(0));
			Assertions.assertThat(resultList.get(2).getItem()).isEqualTo(items.get(2));
			Assertions.assertThat(resultList.get(3).getItem()).isEqualTo(items.get(1));
		}

		private ArrayList<Item> getItems(Long count) {
			return new ArrayList<>(
					em.createQuery("select i from Item i where i.id <= :count order by i.id", Item.class)
							.setParameter("count", count)
							.getResultList());
		}

		private void setFavoriteItemsWithInterval(
				List<Item> items, int interval, int count, Member member, Category category) {
			FavoriteItemWithInterval[] favoriteItemWithIntervals = new FavoriteItemWithInterval[count];
			int i = 0;
			for (Item item : items) {
				favoriteItemWithIntervals[i] =
						FavoriteItemWithInterval.builder()
								.member(member)
								.category(category)
								.item(item)
								.order(i * interval + interval)
								.build();
				i++;
			}

			for (FavoriteItemWithInterval favoriteItemWithInterval : favoriteItemWithIntervals) {
				em.persist(favoriteItemWithInterval);
			}
		}

		private Member getMember(Long memberId) {
			return em.createQuery("select m from Member m where m.id = :id", Member.class)
					.setParameter("id", memberId)
					.getSingleResult();
		}

		private Category getCategory(int interval, Member member) {
			return em.createQuery(
							"select c from Category c "
									+ " where c.member = :member "
									+ " and c.orderInterval = :interval",
							Category.class)
					.setParameter("member", member)
					.setParameter("interval", interval)
					.getSingleResult();
		}
	}

	@Nested
	class SHORT_INTERVAL_TEST {}
}
