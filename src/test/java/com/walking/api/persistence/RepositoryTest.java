package com.walking.api.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walking.api.config.ApiDataSourceConfig;
import com.walking.api.config.ApiEntityConfig;
import com.walking.api.config.ApiJpaConfig;
import com.walking.api.config.DataJpaConfig;
import com.walking.api.data.entity.IndexedPointEntity;
import com.walking.api.data.entity.IndexedLatLng;
import com.walking.api.data.entity.NonIndexedLatLng;
import com.walking.api.data.entity.NonIndexedPointEntity;
import com.walking.api.persistence.support.DistanceCalculateSupporter;
import com.walking.api.persistence.support.LogSupporter;
import com.walking.api.persistence.support.TestData;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Slf4j
@ActiveProfiles(profiles = {"test"})
@DataJpaTest(
		excludeAutoConfiguration = {
			DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class,
			HibernateJpaAutoConfiguration.class,
		})
@TestPropertySource(locations = "classpath:application-test.yml")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(
		classes = {
			ApiDataSourceConfig.class,
			ApiEntityConfig.class,
			ApiJpaConfig.class,
			DataJpaConfig.class,
			ObjectMapper.class,
			LogSupporter.class,
			DistanceCalculateSupporter.class
		})
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RepositoryTest implements ApplicationContextAware {

	@Autowired EntityManager em;

	@Autowired LogSupporter logSupporter;

	@Autowired DistanceCalculateSupporter distanceCalculateSupporter;

	static double LAT = TestData.ONE.getLat();
	static double LNG = TestData.ONE.getLng();

	static int query_idx;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
		List<String> profiles = Arrays.asList(activeProfiles);
		if (profiles.contains("mysql-local")) {
			query_idx = 0;
		} else if (profiles.contains("postgresql-local")) {
			query_idx = 1;
		}
	}

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
						new Coordinate(LNG + double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT - double_difference),
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
						new Coordinate(LNG + double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT - double_difference),
					};
			Polygon square = geometryFactory.createPolygon(vertexes);
			String mysql_query =
					"SELECT * FROM point_tb WHERE ST_CONTAINS(:square, point_value) AND ST_DISTANCE_SPHERE(:point, point_value) <= :distance";
			String query = mysql_query;

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
						new Coordinate(LNG + double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT - double_difference),
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
						new Coordinate(LNG + double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT + double_difference),
						new Coordinate(LNG - double_difference, LAT - double_difference),
						new Coordinate(LNG + double_difference, LAT - double_difference),
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
