package com.walking.api.data.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 신호등 */
@EqualsAndHashCode
@Entity
@Data
public class Traffic {

	@Id private Long id;
}
