package com.walking.api.data.persistence;

import com.walking.api.data.entity.SequenceIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SequenceIdRepository extends JpaRepository<SequenceIdEntity, Long> {}
