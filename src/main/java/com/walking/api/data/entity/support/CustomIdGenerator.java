package com.walking.api.data.entity.support;

import java.io.Serializable;
import java.util.Properties;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

public class CustomIdGenerator extends UUIDGenerator {

	public static final String NAME = "idGenerator";
	public static final String STRATEGY = "com.walking.api.data.entity.support.CustomIdGenerator";
	public static final String DOMAIN = "DOMAIN";

	private String domain;

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) {
		return domain + "-" + super.generate(session, object);
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
		this.domain = ConfigurationHelper.getString(DOMAIN, params);
		super.configure(type, params, serviceRegistry);
	}
}
