package com.walking.api.data.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity(name = "NonIndexedPointEntity")
@EntityListeners({AuditingEntityListener.class})
@ToString
@Builder
@Table(name = "non_idx_point_tb")
public class NonIndexedPointEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	//	@Column(name = "point_value", columnDefinition = "POINT SRID 4326", nullable = false)
	@Column(name = "point_value", nullable = false)
	private Point pointValue;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NonIndexedPointEntity that = (NonIndexedPointEntity) o;
		boolean namesEqual = Objects.equals(name, that.name);

		BigDecimal thisX = BigDecimal.valueOf(this.pointValue.getX()).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thisY = BigDecimal.valueOf(this.pointValue.getY()).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thatX = BigDecimal.valueOf(that.pointValue.getX()).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thatY = BigDecimal.valueOf(that.pointValue.getY()).setScale(6, RoundingMode.HALF_UP);

		boolean pointsEqual = thisX.equals(thatX) && thisY.equals(thatY);

		return namesEqual && pointsEqual;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				name,
				BigDecimal.valueOf(this.pointValue.getX()).setScale(6, RoundingMode.HALF_UP),
				BigDecimal.valueOf(this.pointValue.getY()).setScale(6, RoundingMode.HALF_UP));
	}
}
