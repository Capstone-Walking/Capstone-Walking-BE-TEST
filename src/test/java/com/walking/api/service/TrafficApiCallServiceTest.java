package com.walking.api.service;

import com.walking.api.ApiApp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ActiveProfiles(value = "test")
@SpringBootTest
@ContextConfiguration(classes = {ApiApp.class})
class TrafficApiCallServiceTest {

	@Autowired TrafficApiCallService trafficApiCallService;
	int executionNumber;

	@Rollback(false)
	@Transactional
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
	void 신호_색상과_잔여시간_데이터를_생성한다(int number) {
		executionNumber =
				number; // traffic_api_call 테이블에 존재하는 마지막 execution_number 의 + 1된 값으로 실행시켜야합니다.
		trafficApiCallService.execute(60, executionNumber);
	}
}
