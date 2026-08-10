package com.dcim.connectivity.latency;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LatencyHistoryRepository extends JpaRepository<LatencyHistory, Long> {

	@Query("""
			select l from LatencyHistory l
			where l.validTo is null
			order by l.latencyName
			""")
	List<LatencyHistory> findCurrent();

	@Query("""
			select l from LatencyHistory l
			where l.latencyIdentity.latencyId = :latencyId and l.validTo is null
			""")
	Optional<LatencyHistory> findCurrentByLatencyId(@Param("latencyId") Long latencyId);

	List<LatencyHistory> findByLatencyIdentity_LatencyIdOrderByLatencyHistoryIdAsc(Long latencyId);
}
