package com.dcim.site.rack;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackViewRepository extends JpaRepository<RackView, Long> {

	@Query("""
			select r from RackView r
			where r.validTo is null
			order by r.rackName
			""")
	List<RackView> findCurrentRacks();

	@Query("""
			select r from RackView r
			where r.rackIdentity.rackId = :rackId and r.validTo is null
			""")
	Optional<RackView> findCurrentByRackId(@Param("rackId") Long rackId);

	@Query("""
			select r from RackView r
			where r.cageId = :cageId and r.validTo is null
			order by r.rackName
			""")
	List<RackView> findCurrentByCageId(@Param("cageId") Long cageId);

	@Query("""
			select r from RackView r
			where r.dataCenterId = :dataCenterId and r.validTo is null
			order by r.rackName
			""")
	List<RackView> findCurrentByDataCenterId(@Param("dataCenterId") Long dataCenterId);

	List<RackView> findByRackIdentity_RackIdOrderByRackHistoryIdAsc(Long rackId);
}
