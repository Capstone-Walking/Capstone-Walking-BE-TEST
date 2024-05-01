package com.walking.api.data.entity;

import com.walking.api.data.entity.support.listener.SoftDeleteListener;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Entity
@EntityListeners({AuditingEntityListener.class, SoftDeleteListener.class})
@Builder(toBuilder = true)
public class SequenceIdEntity {

	@Id
	@SequenceGenerator(
			name = "sequence_id_generator",
			sequenceName = "sequence_id",
			initialValue = 1,
			allocationSize = 1000)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence_id_generator")
	private Long id;

	@Builder.Default
	@Column(nullable = false)
	private Boolean deleted = false;

	public void delete() {
		this.deleted = true;
	}
}
