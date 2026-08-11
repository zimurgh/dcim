package com.dcim.connectivity.crossconnect;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CrossConnectViewRepository extends JpaRepository<CrossConnectView, Long> {

	@Query("""
			select c from CrossConnectView c
			where c.validTo is null
			order by c.crossConnectName
			""")
	List<CrossConnectView> findCurrent();

	@Query("""
			select c from CrossConnectView c
			where c.crossConnectIdentity.crossConnectId = :crossConnectId and c.validTo is null
			""")
	Optional<CrossConnectView> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	@Query("""
			select c from CrossConnectView c
			where c.latencyId = :latencyId and c.validTo is null
			""")
	List<CrossConnectView> findCurrentByLatencyId(@Param("latencyId") Long latencyId);

	@Query("""
			select c from CrossConnectView c
			where c.speedId = :speedId and c.validTo is null
			""")
	List<CrossConnectView> findCurrentBySpeedId(@Param("speedId") Long speedId);

	@Query("""
			select c from CrossConnectView c
			where c.crossConnectTypeId = :crossConnectTypeId and c.validTo is null
			""")
	List<CrossConnectView> findCurrentByCrossConnectTypeId(@Param("crossConnectTypeId") Long crossConnectTypeId);

	List<CrossConnectView> findByCrossConnectIdentity_CrossConnectIdOrderByCrossConnectHistoryIdAsc(
			Long crossConnectId);
}
