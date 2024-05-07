package com.walking.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {TrafficApiCallService.class})
@Rollback(value = false)
class TrafficApiCallServiceTest extends RepositoryTest {

	@Autowired TrafficApiCallService trafficApiCallService;
	int executionNumber;

	@Test
	void 신호_색상과_잔여시간_데이터를_생성한다() {
		executionNumber = 2; // traffic_api_call 테이블에 존재하는 마지막 execution_number 의 + 1된 값으로 실행시켜야합니다.
		trafficApiCallService.execute(60, executionNumber);
	}
}
