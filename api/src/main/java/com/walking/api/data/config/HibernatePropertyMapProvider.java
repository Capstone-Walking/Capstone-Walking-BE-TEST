package com.walking.api.data.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HibernatePropertyMapProvider {

	private final HibernateProperties hibernateProperties;
	private final JpaProperties jpaProperties;

	public Map<String, Object> get() {
		return hibernateProperties.determineHibernateProperties(
				jpaProperties.getProperties(), new HibernateSettings());
	}
}
