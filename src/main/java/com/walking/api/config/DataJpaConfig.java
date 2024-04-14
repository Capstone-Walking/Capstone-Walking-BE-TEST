package com.walking.api.config;

import static com.walking.api.config.ApiAppConfig.BEAN_NAME_PREFIX;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
@EnableAutoConfiguration(exclude = HibernateJpaAutoConfiguration.class)
public class DataJpaConfig {

	public static final String JPA_PROPERTIES = BEAN_NAME_PREFIX + "JpaProperties";
	public static final String HIBERNATE_PROPERTIES = BEAN_NAME_PREFIX + "HibernateProperties";
	public static final String JPA_VENDOR_ADAPTER = BEAN_NAME_PREFIX + "JpaVendorAdapter";
	public static final String ENTITY_MANAGER_FACTORY_BUILDER =
			BEAN_NAME_PREFIX + "ManagerFactoryBuilder";
	public static final String HIBERNATE_PROPERTY_MAP_PROVIDER =
			BEAN_NAME_PREFIX + "HibernatePropertyMapProvider";

	@Bean(name = JPA_PROPERTIES)
	@ConfigurationProperties("spring.jpa")
	public JpaProperties jpaProperties() {
		return new JpaProperties();
	}

	@Bean(name = HIBERNATE_PROPERTIES)
	@ConfigurationProperties("spring.jpa.hibernate")
	public HibernateProperties hibernateProperties() {
		return new HibernateProperties();
	}

	@Bean(name = JPA_VENDOR_ADAPTER)
	public JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Bean(name = ENTITY_MANAGER_FACTORY_BUILDER)
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
			JpaVendorAdapter jpaVendorAdapter,
			ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {

		Map<String, String> jpaPropertyMap = jpaProperties().getProperties();
		return new EntityManagerFactoryBuilder(
				jpaVendorAdapter, jpaPropertyMap, persistenceUnitManager.getIfAvailable());
	}

	@Bean(name = HIBERNATE_PROPERTY_MAP_PROVIDER)
	public HibernatePropertyMapProvider hibernatePropertyMapProvider() {
		return new HibernatePropertyMapProvider(hibernateProperties(), jpaProperties());
	}

	@RequiredArgsConstructor
	public static class HibernatePropertyMapProvider {

		private final HibernateProperties hibernateProperties;
		private final JpaProperties jpaProperties;

		public Map<String, Object> get() {
			return hibernateProperties.determineHibernateProperties(
					jpaProperties.getProperties(), new HibernateSettings());
		}
	}
}
