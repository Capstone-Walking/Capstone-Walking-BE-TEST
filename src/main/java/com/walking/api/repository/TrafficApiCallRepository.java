package com.walking.api.repository;

import com.walking.api.data.entity.Traffic;
import com.walking.api.data.entity.TrafficApiCall;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficApiCallRepository extends JpaRepository<TrafficApiCall, Long> {

	@Query(
			"select fac from TrafficApiCall fac "
					+ " join fac.traffic "
					+ " where fac.executionNumber = :executionNumber")
	List<TrafficApiCall> findByExecutionNumber(@Param("executionNumber") int executionNumber);

	/**
	 * 요청한 각각의 신호등에 대해 최근 start 번째 데이터 ~ end 번째 데이터를 가져옵니다.
	 *
	 * @param traffics 데이터를 요청할 신호등
	 * @param start 최근 start 번째 데이터 부터
	 * @param end end 번째 데이터까지
	 * @return 요청한 신호등의 최근 start 번째 데이터 ~ end 번째 데이터
	 */
	@Query(
			value =
					"WITH sorted_data AS ( "
							+ "    SELECT *, ROW_NUMBER() OVER (PARTITION BY traffic_id ORDER BY execution_number DESC) AS row_num "
							+ "    FROM traffic_api_call "
							+ "    WHERE traffic_id IN :traffics "
							+ " )"
							+ " SELECT * "
							+ " FROM sorted_data "
							+ " WHERE row_num BETWEEN :start AND :end ",
			nativeQuery = true)
	List<TrafficApiCall> getRecentlyData(
			@Param("traffics") List<Traffic> traffics,
			@Param("start") Integer start,
			@Param("end") Integer end);
}
