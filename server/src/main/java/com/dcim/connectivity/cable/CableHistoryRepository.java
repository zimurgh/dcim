package com.dcim.connectivity.cable;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CableHistoryRepository extends JpaRepository<CableHistory, Long> {

	@Query("""
			select c from CableHistory c
			where c.validTo is null
			order by c.cableName
			""")
	List<CableHistory> findCurrent();

	@Query("""
			select c from CableHistory c
			where c.cableIdentity.cableId = :cableId and c.validTo is null
			""")
	Optional<CableHistory> findCurrentByCableId(@Param("cableId") Long cableId);

	@Query("""
			select c from CableHistory c
			where c.crossConnectIdentity.crossConnectId = :crossConnectId and c.validTo is null
			order by c.cableName
			""")
	List<CableHistory> findCurrentByCrossConnectId(@Param("crossConnectId") Long crossConnectId);

	@Query("""
			select c from CableHistory c
			where c.validTo is null
			  and (c.portAIdentity.rackDevicePortId = :portId or c.portBIdentity.rackDevicePortId = :portId)
			""")
	List<CableHistory> findCurrentByPortId(@Param("portId") Long portId);

	List<CableHistory> findByCableIdentity_CableIdOrderByCableHistoryIdAsc(Long cableId);
}
