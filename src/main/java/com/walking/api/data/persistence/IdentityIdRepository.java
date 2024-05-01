package com.walking.api.data.persistence;

import com.walking.api.data.entity.IdentityIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityIdRepository extends JpaRepository<IdentityIdEntity, Long> {}
