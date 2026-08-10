package com.dcim.connectivity.crossconnect;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CrossConnectHistoryRepository extends JpaRepository<CrossConnectHistory, Long> {

	@Query("""
			select c from CrossConnectHistory c
			where c.validTo is null
			order by c.crossConnectName
			""")
	List<CrossConnectHistory> findCurrent();

	@Query("""
			select c from CrossConnectHistory c
			where c.crossConnectIdentity.crossConnectId = :crossConnectId and c.validTo is null
			""")
	Optional<CrossConnectHistory> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	@Query("""
			select c from CrossConnectHistory c
			where c.circuitId = :circuitId and c.validTo is null
			""")
	List<CrossConnectHistory> findCurrentByCircuitId(@Param("circuitId") String circuitId);

	@Query("""
			select c from CrossConnectHistory c
			where c.latencyIdentity.latencyId = :latencyId and c.validTo is null
			""")
	List<CrossConnectHistory> findCurrentByLatencyId(@Param("latencyId") Long latencyId);

	@Query("""
			select c from CrossConnectHistory c
			where c.speedIdentity.speedId = :speedId and c.validTo is null
			""")
	List<CrossConnectHistory> findCurrentBySpeedId(@Param("speedId") Long speedId);

	@Query("""
			select c from CrossConnectHistory c
			where c.crossConnectTypeIdentity.crossConnectTypeId = :crossConnectTypeId and c.validTo is null
			""")
	List<CrossConnectHistory> findCurrentByCrossConnectTypeId(@Param("crossConnectTypeId") Long crossConnectTypeId);

	List<CrossConnectHistory> findByCrossConnectIdentity_CrossConnectIdOrderByCrossConnectHistoryIdAsc(
			Long crossConnectId);
}
