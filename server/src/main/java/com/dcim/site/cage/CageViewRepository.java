package com.dcim.site.cage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CageViewRepository extends JpaRepository<CageView, Long> {

	@Query("""
			select c from CageView c
			where c.validTo is null
			order by c.cageName
			""")
	List<CageView> findCurrentCages();

	@Query("""
			select c from CageView c
			where c.cageIdentity.cageId = :cageId and c.validTo is null
			""")
	Optional<CageView> findCurrentByCageId(@Param("cageId") Long cageId);

	@Query("""
			select c from CageView c
			where c.dataCenterId = :dataCenterId and c.validTo is null
			order by c.cageName
			""")
	List<CageView> findCurrentByDataCenterId(@Param("dataCenterId") Long dataCenterId);

	List<CageView> findByCageIdentity_CageIdOrderByCageHistoryIdAsc(Long cageId);
}
