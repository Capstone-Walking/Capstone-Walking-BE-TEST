package com.walking.api.utils;

import com.walking.api.data.entity.Category;
import com.walking.api.data.entity.FavoriteItemWithInterval;
import com.walking.api.data.entity.Item;
import com.walking.api.data.entity.Member;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataGenerator {

	private final EntityManager em;

	/**
	 * category 에 count 개 만큼의 테스트 데이터를 insert 합니다
	 *
	 * @param category 테스트 데이터를 저장할 카테고리
	 * @param count 생성할 테스트 데이터 개수
	 */
	@Transactional
	public void executeWithInterval(Category category, int interval, int count) {
		Member findMember = em.find(Member.class, 1L);

		FavoriteItemWithInterval[] request = new FavoriteItemWithInterval[count];
		Long id = 1L;
		for (int i = 0; i < count; i++) {
			Item item = em.find(Item.class, id++);
			request[i] =
					FavoriteItemWithInterval.builder()
							.member(findMember)
							.category(category)
							.item(item)
							.order(i * interval + interval)
							.build();
		}

		for (FavoriteItemWithInterval data : request) {
			em.persist(data);
		}
	}
}
