package com.dcim.site.cage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CageHistoryRepository extends JpaRepository<CageHistory, Long> {

	@Query("""
			select c from CageHistory c
			where c.validTo is null
			order by c.cageName
			""")
	List<CageHistory> findCurrentCages();

	@Query("""
			select c from CageHistory c
			where c.cageIdentity.cageId = :cageId and c.validTo is null
			""")
	Optional<CageHistory> findCurrentByCageId(@Param("cageId") Long cageId);

	@Query("""
			select c from CageHistory c
			where c.dataCenterIdentity.dataCenterId = :dataCenterId and c.validTo is null
			order by c.cageName
			""")
	List<CageHistory> findCurrentByDataCenterId(@Param("dataCenterId") Long dataCenterId);

	List<CageHistory> findByCageIdentity_CageIdOrderByCageHistoryIdAsc(Long cageId);

	@Query("""
			select count(c) > 0 from CageHistory c
			where c.validTo is null and c.status = 'Active'
			and lower(c.cageName) = lower(:name)
			and c.dataCenterIdentity.dataCenterId = :dataCenterId
			and (:excludeId is null or c.cageIdentity.cageId <> :excludeId)
			""")
	boolean existsActiveNameClashInDataCenter(
			@Param("name") String name,
			@Param("dataCenterId") Long dataCenterId,
			@Param("excludeId") Long excludeId);

	@Query(value = """
			select count(*)
			from T_DATA_CENTER_HISTORY d
			where d.VALID_TO is null and d.STATUS = 'Active'
			and d.DATA_CENTER_ID = :dataCenterId
			""", nativeQuery = true)
	long countActiveDataCenters(@Param("dataCenterId") Long dataCenterId);

	@Query(value = """
			select distinct r.RACK_ID
			from T_RACK_HISTORY r
			where r.VALID_TO is null and r.STATUS = 'Active'
			and r.CAGE_ID = :cageId
			""", nativeQuery = true)
	List<Long> findActiveRackIdsForCage(@Param("cageId") Long cageId);
}
