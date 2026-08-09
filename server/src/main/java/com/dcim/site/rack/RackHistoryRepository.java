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
}
