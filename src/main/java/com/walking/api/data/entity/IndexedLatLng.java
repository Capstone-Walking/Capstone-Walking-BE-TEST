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
import javax.persistence.Index;
import javax.persistence.Table;
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
@Entity(name = "LatLng")
@EntityListeners({AuditingEntityListener.class})
@ToString
@Builder
@Table(
		name = "lat_lng_tb",
		indexes = {
			@Index(name = "idx_lat_lng_tb_lat_value", columnList = "lat_value"),
			@Index(name = "idx_lat_lng_tb_lng_value", columnList = "lng_value")
		})
public class IndexedLatLng {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "lat_value", nullable = false)
	private Double lat;

	@Column(name = "lng_value", nullable = false)
	private Double lng;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IndexedLatLng that = (IndexedLatLng) o;
		BigDecimal thisLat = BigDecimal.valueOf(this.lat).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thisLng = BigDecimal.valueOf(this.lng).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thatLat = BigDecimal.valueOf(that.lat).setScale(6, RoundingMode.HALF_UP);
		BigDecimal thatLng = BigDecimal.valueOf(that.lng).setScale(6, RoundingMode.HALF_UP);
		return Objects.equals(name, that.name) && thisLat.equals(thatLat) && thisLng.equals(thatLng);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				name,
				BigDecimal.valueOf(this.lat).setScale(6, RoundingMode.HALF_UP),
				BigDecimal.valueOf(this.lng).setScale(6, RoundingMode.HALF_UP));
	}
}
