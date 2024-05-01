package com.walking.api.data;

import com.walking.api.ApiApp;
import com.walking.api.data.entity.CustomIdEntity;
import com.walking.api.data.entity.IdentityIdEntity;
import com.walking.api.data.entity.SequenceIdEntity;
import com.walking.api.data.entity.UUIDIdEntity;
import com.walking.api.data.persistence.CustomIdRepository;
import com.walking.api.data.persistence.IdentityIdRepository;
import com.walking.api.data.persistence.SequenceIdRepository;
import com.walking.api.data.persistence.UUIDIdRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ActiveProfiles({"test"})
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@ContextConfiguration(classes = {ApiApp.class})
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class IdGeneratorTest {

	@Autowired CustomIdRepository customIdRepository;

	@Autowired IdentityIdRepository identityIdRepository;

	@Autowired SequenceIdRepository sequenceIdRepository;

	@Autowired UUIDIdRepository uuidIdRepository;

	static int SPLIT = 1000;

	static int COUNT = 100000;

	static int DIV = 777;

	static List<Long> IDENTITY_IDS = new ArrayList<>();

	static List<Long> SEQUENCE_IDS = new ArrayList<>();

	static List<String> UUID_IDS = new ArrayList<>();

	static List<String> CUSTOM_IDS = new ArrayList<>();

	@Nested
	@Transactional
	@Rollback(false)
	@Order(1)
	class Write {

		@Test
		void IDENTITY전략_JPA() {
			identityIdRepository.deleteAllInBatch();

			List<Integer> temps = new ArrayList<>();
			List<IdentityIdEntity> sources = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			int t = 0;
			for (int i = 0; i < COUNT; i++) {
				IdentityIdEntity entity = IdentityIdEntity.builder().build();
				sources.add(entity);
				// 일부 데이터를 저장하기 위해 저장 순서를 임시 저장합니다.
				if ((i % DIV) == 0) {
					temps.add(i - t);
				}
				// SPLIT 단위로 엔티티를 저장합니다.
				if (i % SPLIT == 0) {
					identityIdRepository.saveAll(sources);
					for (int j : temps) {
						IDENTITY_IDS.add(sources.get(j).getId());
					}
					sources.clear();
					temps.clear();
					t = i + 1;
				}
			}
			identityIdRepository.saveAll(sources);
			long time = stopWatch.getTime();
			stopWatch.stop();
			for (int i = 0; i < temps.size(); i++) {
				IDENTITY_IDS.add(sources.get(temps.get(i)).getId());
			}
			log.info("stopWatch: {} ms", time);
		}

		@Test
		void UUID2전략_JPA() {
			uuidIdRepository.deleteAllInBatch();

			List<Integer> temps = new ArrayList<>();
			List<UUIDIdEntity> sources = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			int t = 0;
			for (int i = 0; i < COUNT; i++) {
				UUIDIdEntity entity = UUIDIdEntity.builder().build();
				sources.add(entity);
				// 일부 데이터를 저장하기 위해 저장 순서를 임시 저장합니다.
				if ((i % DIV) == 0) {
					temps.add(i - t);
				}
				// SPLIT 단위로 엔티티를 저장합니다.
				if (i % SPLIT == 0) {
					uuidIdRepository.saveAll(sources);
					for (int j : temps) {
						UUID_IDS.add(sources.get(j).getId());
					}
					sources.clear();
					temps.clear();
					t = i + 1;
				}
			}
			uuidIdRepository.saveAll(sources);
			long time = stopWatch.getTime();
			stopWatch.stop();
			for (Integer temp : temps) {
				UUID_IDS.add(sources.get(temp).getId());
			}
			log.info("stopWatch: {} ms", time);
		}

		@Test
		void SEQUENCE전략_JPA() {
			sequenceIdRepository.deleteAllInBatch();

			List<Integer> temps = new ArrayList<>();
			List<SequenceIdEntity> sources = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			int t = 0;
			for (int i = 0; i < COUNT; i++) {
				SequenceIdEntity entity = SequenceIdEntity.builder().build();
				sources.add(entity);
				// 일부 데이터를 저장하기 위해 저장 순서를 임시 저장합니다.
				if ((i % DIV) == 0) {
					temps.add(i - t);
				}
				// SPLIT 단위로 엔티티를 저장합니다.
				if (i % SPLIT == 0) {
					sequenceIdRepository.saveAll(sources);
					for (int j : temps) {
						SEQUENCE_IDS.add(sources.get(j).getId());
					}
					sources.clear();
					temps.clear();
					t = i + 1;
				}
			}
			sequenceIdRepository.saveAll(sources);
			long time = stopWatch.getTime();
			stopWatch.stop();
			for (Integer temp : temps) {
				SEQUENCE_IDS.add(sources.get(temp).getId());
			}
			log.info("stopWatch: {} ms", time);
		}

		@Test
		void CUSTOM전략_JPA() {
			customIdRepository.deleteAllInBatch();

			List<Integer> temps = new ArrayList<>();
			List<CustomIdEntity> sources = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			int t = 0;
			for (int i = 0; i < COUNT; i++) {
				CustomIdEntity entity = CustomIdEntity.builder().build();
				sources.add(entity);
				// 일부 데이터를 저장하기 위해 저장 순서를 임시 저장합니다.
				if ((i % DIV) == 0) {
					temps.add(i - t);
				}
				// SPLIT 단위로 엔티티를 저장합니다.
				if (i % SPLIT == 0) {
					customIdRepository.saveAll(sources);
					for (int j : temps) {
						CUSTOM_IDS.add(sources.get(j).getId());
					}
					sources.clear();
					temps.clear();
					t = i + 1;
				}
			}
			customIdRepository.saveAll(sources);
			long time = stopWatch.getTime();
			stopWatch.stop();
			for (Integer temp : temps) {
				CUSTOM_IDS.add(sources.get(temp).getId());
			}
			log.info("stopWatch: {} ms", time);
		}
	}

	@Nested
	@Transactional(readOnly = true)
	@Order(2)
	class Read {
		@Test
		@SneakyThrows
		void IDENTITY전략_JPA() {
			Thread.sleep(300);
			log.info("ids: {}", IDENTITY_IDS);
			log.info("size: {}", IDENTITY_IDS.size());
			List<Long> readIds = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			for (Long identityId : IDENTITY_IDS) {
				Long id = identityIdRepository.findById(identityId).get().getId();
				readIds.add(id);
			}
			long time = stopWatch.getTime();
			stopWatch.stop();
			Assertions.assertEquals(IDENTITY_IDS, readIds);
			log.info("stopWatch: {} ms", time);
		}

		@Test
		@SneakyThrows
		void UUID2전략_JPA() {
			Thread.sleep(300);
			log.info("ids: {}", UUID_IDS);
			log.info("size: {}", UUID_IDS.size());
			List<String> readIds = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			for (String uuidId : UUID_IDS) {
				String id = uuidIdRepository.findById(uuidId).get().getId();
				readIds.add(id);
			}
			long time = stopWatch.getTime();
			stopWatch.stop();
			Assertions.assertEquals(UUID_IDS, readIds);
			log.info("stopWatch: {} ms", time);
		}

		@Test
		@SneakyThrows
		void SEQUENCE전략_JPA() {
			Thread.sleep(300);
			log.info("ids: {}", SEQUENCE_IDS);
			log.info("size: {}", SEQUENCE_IDS.size());
			List<Long> readIds = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			for (Long sequenceId : SEQUENCE_IDS) {
				Long id = sequenceIdRepository.findById(sequenceId).get().getId();
				readIds.add(id);
			}
			long time = stopWatch.getTime();
			stopWatch.stop();
			Assertions.assertEquals(SEQUENCE_IDS, readIds);
			log.info("stopWatch: {} ms", time);
		}

		@Test
		@SneakyThrows
		void CUSTOM전략_JPA() {
			Thread.sleep(300);
			log.info("ids: {}", CUSTOM_IDS);
			log.info("size: {}", CUSTOM_IDS.size());
			List<String> readIds = new ArrayList<>();
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			for (String customId : CUSTOM_IDS) {
				String id = customIdRepository.findById(customId).get().getId();
				readIds.add(id);
			}
			long time = stopWatch.getTime();
			stopWatch.stop();
			Assertions.assertEquals(CUSTOM_IDS, readIds);
			log.info("stopWatch: {} ms", time);
		}
	}
}
