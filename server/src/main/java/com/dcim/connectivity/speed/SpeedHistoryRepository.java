package com.dcim.connectivity.speed;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpeedHistoryRepository extends JpaRepository<SpeedHistory, Long> {

	@Query("""
			select s from SpeedHistory s
			where s.validTo is null
			order by s.speedName
			""")
	List<SpeedHistory> findCurrent();

	@Query("""
			select s from SpeedHistory s
			where s.speedIdentity.speedId = :speedId and s.validTo is null
			""")
	Optional<SpeedHistory> findCurrentBySpeedId(@Param("speedId") Long speedId);

	List<SpeedHistory> findBySpeedIdentity_SpeedIdOrderBySpeedHistoryIdAsc(Long speedId);
}
