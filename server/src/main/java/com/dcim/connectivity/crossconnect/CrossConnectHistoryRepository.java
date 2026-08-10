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

	List<CrossConnectHistory> findByCrossConnectIdentity_CrossConnectIdOrderByCrossConnectHistoryIdAsc(
			Long crossConnectId);
}
