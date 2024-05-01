package com.walking.api.data.persistence;

import com.walking.api.data.entity.UUIDIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UUIDIdRepository extends JpaRepository<UUIDIdEntity, String> {}
