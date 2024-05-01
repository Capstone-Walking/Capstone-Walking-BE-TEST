package com.walking.api.data.persistence;

import com.walking.api.data.entity.CustomIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomIdRepository extends JpaRepository<CustomIdEntity, String> {}
