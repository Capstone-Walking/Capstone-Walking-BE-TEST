package com.walking.api.persistence;

import com.walking.api.data.entity.IndexedLatLng;
import com.walking.api.data.entity.NonIndexedLatLng;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.util.Stopwatch;

@Slf4j
class LatLngTest extends RepositoryTest {

	@Nested
	class INDEXED_LNG_LAT_TEST {

		@BeforeEach
		void setUp() {
			log.info("============= start setUp =============");
			Double lat = LAT;
			Double lng = LNG;
			TypedQuery<Integer> query =
					em.createQuery(
							"select 1 from LatLng ll where ll.lat=:lat and ll.lng=:lng", Integer.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			List<Integer> resultList = query.getResultList();
			log.info("============= finish setUp =============");
		}

		@Test
		void LAT_LNG로_위치를_조회합니다() {
			// Given
			Double lat = LAT;
			Double lng = LNG;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<IndexedLatLng> query =
					em.createQuery(
							"select ll from LatLng ll where ll.lat=:lat and ll.lng=:lng", IndexedLatLng.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			IndexedLatLng latLng = query.getResultList().get(0);
			long queryTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			Assertions.assertEquals(lng, latLng.getLng());
			Assertions.assertEquals(lat, latLng.getLat());
			log.info(latLng.toString());
			logSupporter.queryTimeLog(queryTime);
		}

		@ParameterizedTest(name = "{0}와 {1}의 거리는 {2}m 이내여야 합니다.")
		@CsvSource({"영통역6번출구.영덕고등학교, 영통홈플러스, 300.0", "계양도서관, 왕궁가든, 500.0"})
		void 두_LAT_LNG_위치의_거리를_계산합니다(String location1, String location2, double distance) {
			//	Given
			TypedQuery<IndexedLatLng> query =
					em.createQuery("select ll from LatLng ll where ll.name =:location", IndexedLatLng.class);
			query.setParameter("location", location1);
			IndexedLatLng p1 = query.getResultList().get(0);
			query.setParameter("location", location2);
			IndexedLatLng p2 = query.getResultList().get(0);

			//	When
			double lat1 = p1.getLat();
			double lng1 = p1.getLng();
			double lat2 = p2.getLat();
			double lng2 = p2.getLng();
			TypedQuery<Object> emQuery =
					em.createQuery(
							"SELECT ((6371 * acos(cos(radians(:lat1)) * cos(radians(:lat2)) * cos(radians(:lng2) - radians(:lng1)) + sin(radians(:lat1)) * sin(radians(:lat2)))) * 1000) AS distance FROM LatLng WHERE id = 1",
							Object.class);
			emQuery.setParameter("lat1", lat1);
			emQuery.setParameter("lat2", lat2);
			emQuery.setParameter("lng1", lng1);
			emQuery.setParameter("lng2", lng2);
			Object sql_distance = emQuery.getSingleResult();

			//	Then
			log.info("{}의 좌표: {}, {}", p1.getName(), lat1, lng1);
			log.info("{}의 좌표: {}, {}", p2.getName(), lat2, lng2);
			log.info("[SQL] {}과 {}의 거리는 {}m 입니다.", p1.getName(), p2.getName(), sql_distance);
		}

		@ParameterizedTest(name = "거리: {0}")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void LAT_LNG로_일정_거리_이내의_위치를_계산_후_조회합니다(double p_distance) {
			// Given
			Double lat = LAT;
			Double lng = LNG;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<IndexedLatLng> query =
					em.createQuery(
							"SELECT ll FROM LatLng ll WHERE "
									+ "(6371 * acos(cos(radians(:lat)) * cos(radians(ll.lat)) * cos(radians(ll.lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(ll.lat)))) * 1000 <= :distance",
							IndexedLatLng.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			query.setParameter("distance", p_distance);
			List<IndexedLatLng> all = query.getResultList();
			long queryTime = stopwatch.getTime();
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (IndexedLatLng latLng : all) {
				double distance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, latLng.getLat().doubleValue(), latLng.getLng().doubleValue());
				Assertions.assertTrue(distance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(all.size());
			logSupporter.sizeLog(all.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void LAT_LNG로_필터링한_값을_테이블로_만들어_조인하고_일정_거리_이내의_위치를_계산_후_조회합니다(double p_distance) {
			// Given
			Double double_difference = p_distance / (111.32 * 1000);
			Double lat = LAT;
			Double lat1 = lat - (double_difference);
			Double lat2 = lat + (double_difference);
			Double lng = LNG;
			Double lng1 = lng - (double_difference);
			Double lng2 = lng + (double_difference);

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery =
					em.createNativeQuery(
							"SELECT ll.* FROM lat_lng_tb ll JOIN( SELECT id FROM lat_lng_tb WHERE lat_lng_tb.lat_value BETWEEN :lat1 AND :lat2 AND lat_lng_tb.lng_value BETWEEN :lng1 AND  :lng2) AS ill ON ll.id = ill.id WHERE "
									+ "(6371 * acos(cos(radians(:lat)) * cos(radians(ll.lat_value)) * cos(radians(ll.lng_value) - radians(:lng)) + sin(radians(:lat)) * sin(radians(ll.lat_value)))) * 1000 <= :distance",
							IndexedLatLng.class);
			nativeQuery.setParameter("lat", lat);
			nativeQuery.setParameter("lat1", lat1);
			nativeQuery.setParameter("lat2", lat2);
			nativeQuery.setParameter("lng", lng);
			nativeQuery.setParameter("lng1", lng1);
			nativeQuery.setParameter("lng2", lng2);
			nativeQuery.setParameter("distance", p_distance);
			List<IndexedLatLng> all = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (IndexedLatLng latLng : all) {
				double distance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, latLng.getLat().doubleValue(), latLng.getLng().doubleValue());
				Assertions.assertTrue(distance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(all.size());
			logSupporter.sizeLog(all.stream().distinct().count());
		}
	}

	@Nested
	class NON_INDEXED_LNG_LAT_TEST {

		@BeforeEach
		void setUp() {
			log.info("============= start setUp =============");
			Double lat = LAT;
			Double lng = LNG;
			TypedQuery<Integer> query =
					em.createQuery(
							"select 1 from NonIndexedLatLng ll where ll.lat=:lat and ll.lng=:lng", Integer.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			List<Integer> resultList = query.getResultList();
			log.info("============= finish setUp =============");
		}

		@Test
		void LAT_LNG로_위치를_조회합니다() {
			// Given
			Double lat = LAT;
			Double lng = LNG;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<NonIndexedLatLng> query =
					em.createQuery(
							"select ll from NonIndexedLatLng ll where ll.lat=:lat and ll.lng=:lng",
							NonIndexedLatLng.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			NonIndexedLatLng latLng = query.getResultList().get(0);
			long queryTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			Assertions.assertEquals(lng, latLng.getLng());
			Assertions.assertEquals(lat, latLng.getLat());
			log.info(latLng.toString());
			logSupporter.queryTimeLog(queryTime);
		}

		@ParameterizedTest(name = "거리: {0}")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void LAT_LNG로_일정_거리_이내의_위치를_계산_후_조회합니다(double p_distance) {
			// Given
			Double lat = LAT;
			Double lng = LNG;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<NonIndexedLatLng> query =
					em.createQuery(
							"SELECT ll FROM NonIndexedLatLng ll WHERE "
									+ "(6371 * acos(cos(radians(:lat)) * cos(radians(ll.lat)) * cos(radians(ll.lng) - radians(:lng)) + sin(radians(:lat)) * sin(radians(ll.lat)))) * 1000 <= :distance",
							NonIndexedLatLng.class);
			query.setParameter("lat", lat);
			query.setParameter("lng", lng);
			query.setParameter("distance", p_distance);
			List<NonIndexedLatLng> all = query.getResultList();
			long queryTime = stopwatch.getTime();
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (NonIndexedLatLng latLng : all) {
				double distance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, latLng.getLat().doubleValue(), latLng.getLng().doubleValue());
				Assertions.assertTrue(distance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(all.size());
			logSupporter.sizeLog(all.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void LAT_LNG로_필터링한_값을_테이블로_만들어_조인하고_일정_거리_이내의_위치를_계산_후_조회합니다(double p_distance) {
			// Given
			Double double_difference = p_distance / (111.32 * 1000);
			Double lat = LAT;
			Double lat1 = lat - (double_difference);
			Double lat2 = lat + (double_difference);
			Double lng = LNG;
			Double lng1 = lng - (double_difference);
			Double lng2 = lng + (double_difference);

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery =
					em.createNativeQuery(
							"SELECT ll.* FROM non_idx_lat_lng_tb ll JOIN( SELECT id FROM non_idx_lat_lng_tb WHERE non_idx_lat_lng_tb.lat_value BETWEEN :lat1 AND :lat2 AND non_idx_lat_lng_tb.lng_value BETWEEN :lng1 AND  :lng2) AS ill ON ll.id = ill.id WHERE "
									+ "(6371 * acos(cos(radians(:lat)) * cos(radians(ll.lat_value)) * cos(radians(ll.lng_value) - radians(:lng)) + sin(radians(:lat)) * sin(radians(ll.lat_value)))) * 1000 <= :distance",
							NonIndexedLatLng.class);
			nativeQuery.setParameter("lat", lat);
			nativeQuery.setParameter("lat1", lat1);
			nativeQuery.setParameter("lat2", lat2);
			nativeQuery.setParameter("lng", lng);
			nativeQuery.setParameter("lng1", lng1);
			nativeQuery.setParameter("lng2", lng2);
			nativeQuery.setParameter("distance", p_distance);
			List<NonIndexedLatLng> all = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (NonIndexedLatLng latLng : all) {
				double distance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, latLng.getLat().doubleValue(), latLng.getLng().doubleValue());
				Assertions.assertTrue(distance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(all.size());
			logSupporter.sizeLog(all.stream().distinct().count());
		}
	}
}
