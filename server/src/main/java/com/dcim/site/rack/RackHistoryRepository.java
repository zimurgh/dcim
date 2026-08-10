package com.dcim.site.rack;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackHistoryRepository extends JpaRepository<RackHistory, Long> {

	@Query("""
			select r from RackHistory r
			where r.validTo is null
			order by r.rackName
			""")
	List<RackHistory> findCurrentRacks();

	@Query("""
			select r from RackHistory r
			where r.rackIdentity.rackId = :rackId and r.validTo is null
			""")
	Optional<RackHistory> findCurrentByRackId(@Param("rackId") Long rackId);

	@Query("""
			select r from RackHistory r
			where r.cageIdentity.cageId = :cageId and r.validTo is null
			order by r.rackName
			""")
	List<RackHistory> findCurrentByCageId(@Param("cageId") Long cageId);

	List<RackHistory> findByRackIdentity_RackIdOrderByRackHistoryIdAsc(Long rackId);

	@Query("""
			select count(r) > 0 from RackHistory r
			where r.validTo is null and r.status = 'Active'
			and lower(r.rackName) = lower(:name)
			and r.cageIdentity.cageId = :cageId
			and (:excludeId is null or r.rackIdentity.rackId <> :excludeId)
			""")
	boolean existsActiveNameClashInCage(
			@Param("name") String name, @Param("cageId") Long cageId, @Param("excludeId") Long excludeId);

	@Query(value = """
			select count(*)
			from T_CAGE_HISTORY c
			where c.VALID_TO is null and c.STATUS = 'Active'
			and c.CAGE_ID = :cageId
			""", nativeQuery = true)
	long countActiveCages(@Param("cageId") Long cageId);

	@Query(value = """
			select distinct d.RACK_DEVICE_ID
			from T_RACK_DEVICE_HISTORY d
			where d.VALID_TO is null and d.STATUS = 'Active'
			and d.RACK_ID = :rackId
			""", nativeQuery = true)
	List<Long> findActiveRackDeviceIdsForRack(@Param("rackId") Long rackId);
}
