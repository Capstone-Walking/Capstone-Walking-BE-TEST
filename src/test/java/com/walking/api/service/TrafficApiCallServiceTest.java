package com.walking.api.service;

import com.walking.api.ApiApp;
import lombok.extern.slf4j.Slf4j;
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
	void 신호_색상과_잔여시간_데이터를_생성한다() {
		executionNumber = 2; // traffic_api_call 테이블에 존재하는 마지막 execution_number 의 + 1된 값으로 실행시켜야합니다.
		trafficApiCallService.execute(60, executionNumber);
	}
}
