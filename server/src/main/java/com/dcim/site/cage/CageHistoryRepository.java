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
}
