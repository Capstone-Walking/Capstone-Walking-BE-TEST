package com.walking.api.persistence.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;

@Slf4j
@TestComponent
public class LogSupporter {

	public void stopWatchLog(Long stopwatch) {
		log.info("*************************************");
		log.info("Elapsed Time : " + stopwatch + "ms");
		log.info("*************************************");
	}

	public void queryTimeLog(Long stopwatch) {
		log.info("*************************************");
		log.info("Query Time : " + stopwatch + "ms");
		log.info("*************************************");
	}

	public void sizeLog(int size) {
		log.info("*************************************");
		log.info("Size : " + size);
		log.info("*************************************");
	}

	public void sizeLog(long size) {
		log.info("*************************************");
		log.info("Size : " + size);
		log.info("*************************************");
	}
}
