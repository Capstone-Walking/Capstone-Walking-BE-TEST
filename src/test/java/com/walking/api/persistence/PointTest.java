package com.walking.api.persistence;

import com.walking.api.data.entity.IndexedPointEntity;
import com.walking.api.data.entity.NonIndexedPointEntity;
import java.util.List;
import java.util.stream.Collectors;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.Stopwatch;

@Slf4j
class PointTest extends RepositoryTest {

	@Nested
	class INDEXED_POINT_TEST {

		@BeforeEach
		void setUp() {
			log.info("============= start setUp =============");
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			TypedQuery<Integer> query =
					em.createQuery("select 1 from PointEntity pe where pe.pointValue =:point", Integer.class);
			query.setParameter("point", point);
			List<Integer> resultList = query.getResultList();
			log.info("============= finish setUp =============");
		}

		@Test
		void 포인트객체로_위치를_조회합니다() {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<IndexedPointEntity> query =
					em.createQuery(
							"select pe from PointEntity pe where pe.pointValue =:point",
							IndexedPointEntity.class);
			query.setParameter("point", point);
			IndexedPointEntity pointEntity = query.getResultList().get(0);
			long queryTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			log.info(pointEntity.toString());
			Assertions.assertEquals(LNG, pointEntity.getPointValue().getX());
			Assertions.assertEquals(LAT, pointEntity.getPointValue().getY());
			logSupporter.queryTimeLog(queryTime);
		}

		@ParameterizedTest(name = "{0}와 {1}의 거리는 {2}m 이내여야 합니다.")
		@CsvSource({"영통역6번출구.영덕고등학교, 영통홈플러스, 300.0", "계양도서관, 왕궁가든, 500.0"})
		void 두_포인트_객체의_거리를_계산합니다(String location1, String location2, double distance) {
			// Given
			TypedQuery<IndexedPointEntity> query =
					em.createQuery(
							"select pe from PointEntity pe where pe.name =:location", IndexedPointEntity.class);
			query.setParameter("location", location1);
			IndexedPointEntity p1 = query.getResultList().get(0);
			query.setParameter("location", location2);
			IndexedPointEntity p2 = query.getResultList().get(0);
			String mysql_query = "SELECT ST_DISTANCE_SPHERE(:point1, :point2)";
			String postgres_query = "SELECT ST_DISTANCESPHERE(:point1, :point2)";
			String[] queries = {mysql_query, postgres_query};
			String s_query = queries[query_idx];

			// When
			double lat1 = p1.getPointValue().getY();
			double lng1 = p1.getPointValue().getX();
			double lat2 = p2.getPointValue().getY();
			double lng2 = p2.getPointValue().getX();
			double haversineDistance =
					distanceCalculateSupporter.haversineDistance(lat1, lng1, lat2, lng2);

			Point point1 = p1.getPointValue();
			Point point2 = p2.getPointValue();

			Object st_distance_sphere =
					em.createNativeQuery(s_query)
							.setParameter("point1", point1)
							.setParameter("point2", point2)
							.getSingleResult();
			Object st_distance =
					em.createNativeQuery("SELECT ST_DISTANCE(:point1, :point2)")
							.setParameter("point1", point1)
							.setParameter("point2", point2)
							.getSingleResult();

			// Then
			log.info("{}의 좌표: {}, {}", p1.getName(), lat1, lng1);
			log.info("{}의 좌표: {}, {}", p2.getName(), lat2, lng2);
			log.info("[Haversine] {}과 {}의 거리는 {}m 입니다.", p1.getName(), p2.getName(), haversineDistance);
			log.info(
					"[ST_DISTANCE_SPHERE] {}과 {}의 거리는 {}m 입니다.",
					p1.getName(),
					p2.getName(),
					st_distance_sphere);
			log.info("[ST_DISTANCE] {}과 {}의 거리는 {}m 입니다.", p1.getName(), p2.getName(), st_distance);
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 포인트버퍼객체를_생성하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			Geometry geometry = point.buffer(double_difference, 2);
			String mysql_query = "SELECT * FROM point_tb WHERE ST_CONTAINS(:buffered, point_value)";
			String query = mysql_query;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, IndexedPointEntity.class);
			nativeQuery.setParameter("buffered", geometry);
			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<IndexedPointEntity> allPoints =
					resultList.stream().map(IndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (IndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());
				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 사각형을_만들어_필터링_후_포인트_버퍼객체를_활용하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			final Coordinate[] vertexes =
					new Coordinate[] {
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
					};
			Polygon square = geometryFactory.createPolygon(vertexes);
			Geometry geometry = point.buffer(double_difference, 2);
			String mysql_query =
					"SELECT * FROM point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_CONTAINS(:buffered, point_value)";
			String query = mysql_query;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, IndexedPointEntity.class);
			nativeQuery.setParameter("square", square);
			nativeQuery.setParameter("buffered", geometry);
			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<IndexedPointEntity> allPoints =
					resultList.stream().map(IndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (IndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());
				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 사각형을_만들어_필터링_후_ST_DISTANCESPHERE를_활용하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			final Coordinate[] vertexes =
					new Coordinate[] {
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
					};
			Polygon square = geometryFactory.createPolygon(vertexes);
			String mysql_query =
					"SELECT * FROM point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_DISTANCE_SPHERE(:point, point_value) <= :distance";
			String postgres_query =
					"SELECT * FROM point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_DISTANCESPHERE(:point, point_value) <= :distance";
			String[] queries = {mysql_query, postgres_query};
			String query = queries[query_idx];

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, IndexedPointEntity.class);
			nativeQuery.setParameter("square", square);
			nativeQuery.setParameter("point", point);
			nativeQuery.setParameter("distance", p_distance);

			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<IndexedPointEntity> allPoints =
					resultList.stream().map(IndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (IndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());

				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}
	}

	@Nested
	class NON_INDEXED_POINT_TEST {

		@BeforeEach
		void setUp() {
			log.info("============= start setUp =============");
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			TypedQuery<Integer> query =
					em.createQuery(
							"select 1 from NonIndexedPointEntity pe where pe.pointValue =:point", Integer.class);
			query.setParameter("point", point);
			List<Integer> resultList = query.getResultList();
			log.info("============= finish setUp =============");
		}

		@Test
		void 포인트객체로_위치를_조회합니다() {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			TypedQuery<NonIndexedPointEntity> query =
					em.createQuery(
							"select pe from NonIndexedPointEntity pe where pe.pointValue =:point",
							NonIndexedPointEntity.class);
			query.setParameter("point", point);
			NonIndexedPointEntity pointEntity = query.getResultList().get(0);
			long queryTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			log.info(pointEntity.toString());
			Assertions.assertEquals(LNG, pointEntity.getPointValue().getX());
			Assertions.assertEquals(LAT, pointEntity.getPointValue().getY());
			logSupporter.queryTimeLog(queryTime);
		}

		@ParameterizedTest(name = "{0}와 {1}의 거리는 {2}m 이내여야 합니다.")
		@CsvSource({"영통역6번출구.영덕고등학교, 영통홈플러스, 300.0", "계양도서관, 왕궁가든, 500.0"})
		void 두_포인트_객체의_거리를_계산합니다(String location1, String location2, double distance) {
			// Given
			TypedQuery<NonIndexedPointEntity> query =
					em.createQuery(
							"select pe from NonIndexedPointEntity pe where pe.name =:location",
							NonIndexedPointEntity.class);
			query.setParameter("location", location1);
			NonIndexedPointEntity p1 = query.getResultList().get(0);
			query.setParameter("location", location2);
			NonIndexedPointEntity p2 = query.getResultList().get(0);
			String mysql_query = "SELECT ST_DISTANCE_SPHERE(:point1, :point2)";
			String postgres_query = "SELECT ST_DISTANCESPHERE(:point1, :point2)";
			String[] queries = {mysql_query, postgres_query};
			String s_query = queries[query_idx];

			// When
			double lat1 = p1.getPointValue().getY();
			double lng1 = p1.getPointValue().getX();
			double lat2 = p2.getPointValue().getY();
			double lng2 = p2.getPointValue().getX();
			double haversineDistance =
					distanceCalculateSupporter.haversineDistance(lat1, lng1, lat2, lng2);

			Point point1 = p1.getPointValue();
			Point point2 = p2.getPointValue();

			Object st_distance_sphere =
					em.createNativeQuery(s_query)
							.setParameter("point1", point1)
							.setParameter("point2", point2)
							.getSingleResult();
			Object st_distance =
					em.createNativeQuery("SELECT ST_DISTANCE(:point1, :point2)")
							.setParameter("point1", point1)
							.setParameter("point2", point2)
							.getSingleResult();

			// Then
			log.info("{}의 좌표: {}, {}", p1.getName(), lat1, lng1);
			log.info("{}의 좌표: {}, {}", p2.getName(), lat2, lng2);
			log.info("[Haversine] {}과 {}의 거리는 {}m 입니다.", p1.getName(), p2.getName(), haversineDistance);
			log.info(
					"[ST_DISTANCE_SPHERE] {}과 {}의 거리는 {}m 입니다.",
					p1.getName(),
					p2.getName(),
					st_distance_sphere);
			log.info("[ST_DISTANCE] {}과 {}의 거리는 {}m 입니다.", p1.getName(), p2.getName(), st_distance);
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 포인트버퍼객체를_생성하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			Geometry geometry = point.buffer(double_difference, 2);
			String mysql_query =
					"SELECT * FROM non_idx_point_tb WHERE ST_CONTAINS(:buffered, point_value)";
			String query = mysql_query;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, NonIndexedPointEntity.class);
			nativeQuery.setParameter("buffered", geometry);
			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<NonIndexedPointEntity> allPoints =
					resultList.stream().map(NonIndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (NonIndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());
				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 사각형을_만들어_필터링_후_포인트_버퍼객체를_활용하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			final Coordinate[] vertexes =
					new Coordinate[] {
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
					};
			Polygon square = geometryFactory.createPolygon(vertexes);
			Geometry geometry = point.buffer(double_difference, 2);
			String mysql_query =
					"SELECT * FROM non_idx_point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_CONTAINS(:buffered, point_value)";
			String query = mysql_query;

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, NonIndexedPointEntity.class);
			nativeQuery.setParameter("square", square);
			nativeQuery.setParameter("buffered", geometry);
			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<NonIndexedPointEntity> allPoints =
					resultList.stream().map(NonIndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (NonIndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());
				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}

		@ParameterizedTest(name = "거리: {0}m")
		@ValueSource(doubles = {300, 500, 1000, 10000})
		void 사각형을_만들어_필터링_후_ST_DISTANCESPHERE를_활용하여_일정_거리_이내의_위치를_조회합니다(double p_distance) {
			// Given
			GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
			Point point = geometryFactory.createPoint(new Coordinate(LNG, LAT));
			Double double_difference = p_distance / (111.32 * 1000);
			final Coordinate[] vertexes =
					new Coordinate[] {
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT + double_difference),
						new Coordinate(
								LNG - double_difference, LAT - double_difference),
						new Coordinate(
								LNG + double_difference, LAT - double_difference),
					};
			Polygon square = geometryFactory.createPolygon(vertexes);
			String mysql_query =
					"SELECT * FROM non_idx_point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_DISTANCE_SPHERE(:point, point_value) <= :distance";
			String postgres_query =
					"SELECT * FROM non_idx_point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_DISTANCESPHERE(:point, point_value) <= :distance";
			String[] queries = {mysql_query, postgres_query};
			String query = queries[query_idx];

			// When
			Stopwatch stopwatch = new Stopwatch();
			stopwatch.start();
			Query nativeQuery = em.createNativeQuery(query, NonIndexedPointEntity.class);
			nativeQuery.setParameter("square", square);
			nativeQuery.setParameter("point", point);
			nativeQuery.setParameter("distance", p_distance);

			List<Object> resultList = nativeQuery.getResultList();
			long queryTime = stopwatch.getTime();
			List<NonIndexedPointEntity> allPoints =
					resultList.stream().map(NonIndexedPointEntity.class::cast).collect(Collectors.toList());
			long endTime = stopwatch.getTime();
			stopwatch.stop();

			// Then
			for (NonIndexedPointEntity pointEntity : allPoints) {

				double haversineDistance =
						distanceCalculateSupporter.haversineDistance(
								LAT, LNG, pointEntity.getPointValue().getY(), pointEntity.getPointValue().getX());

				Assertions.assertTrue(haversineDistance <= p_distance);
			}
			logSupporter.queryTimeLog(queryTime);
			logSupporter.stopWatchLog(endTime);
			logSupporter.sizeLog(allPoints.size());
			logSupporter.sizeLog(allPoints.stream().distinct().count());
		}
	}
}
