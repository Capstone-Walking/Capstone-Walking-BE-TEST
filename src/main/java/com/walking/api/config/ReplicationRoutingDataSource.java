package com.walking.api.config;

import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		if (isActualTransactionActive() && isCurrentTransactionReadOnly()) {
			log.debug("Routing DataSource to READ");
			return DatabaseType.READ;
		}

		log.debug("Routing DataSource to WRITE");
		return DatabaseType.WRITE;
	}

	public enum DatabaseType {
		READ,
		WRITE,
	}
}
