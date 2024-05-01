package com.walking.api.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
@EnableAutoConfiguration(
		exclude = {
			DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class,
		})
public class ApiDataSourceConfig {

	public static final String DATASOURCE_NAME = ApiAppConfig.BEAN_NAME_PREFIX + "DataSource";

	@Bean(name = DATASOURCE_NAME)
	public DataSource integrationDataSource() {
		return new LazyConnectionDataSourceProxy(integrationRoutingDataSource());
	}

	@Bean
	public DataSource integrationRoutingDataSource() {
		ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();

		routingDataSource.setDefaultTargetDataSource(integrationWriteDataSource());
		routingDataSource.setTargetDataSources(getIntegrationTargetDataSources());

		return routingDataSource;
	}

	private Map<Object, Object> getIntegrationTargetDataSources() {
		Map<Object, Object> targetDataSourceMap = new HashMap<>();

		targetDataSourceMap.put(
				ReplicationRoutingDataSource.DatabaseType.READ, integrationReadDataSource());
		targetDataSourceMap.put(
				ReplicationRoutingDataSource.DatabaseType.WRITE, integrationWriteDataSource());

		return targetDataSourceMap;
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.write")
	public DataSource integrationWriteDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.read")
	public DataSource integrationReadDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}
}
