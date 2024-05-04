package com.walking.api.data.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

/** 신호등 */
@Entity
@Data
public class Traffic {

	@Id private Long id;
}
