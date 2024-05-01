package com.walking.api.data.entity;

import com.walking.api.data.entity.support.listener.SoftDeleteListener;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@Entity
@EntityListeners({AuditingEntityListener.class, SoftDeleteListener.class})
@Builder(toBuilder = true)
public class UUIDIdEntity {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(length = 36)
	private String id;

	@Builder.Default
	@Column(nullable = false)
	private Boolean deleted = false;

	public void delete() {
		this.deleted = true;
	}
}
