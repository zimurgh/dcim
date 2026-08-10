package com.dcim.site.datacenter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DataCenterHistoryRepository extends JpaRepository<DataCenterHistory, Long> {

	@Query("""
			select d from DataCenterHistory d
			where d.validTo is null
			order by d.dataCenterName
			""")
	List<DataCenterHistory> findCurrentDataCenters();

	@Query("""
			select d from DataCenterHistory d
			where d.dataCenterIdentity.dataCenterId = :dataCenterId and d.validTo is null
			""")
	Optional<DataCenterHistory> findCurrentByDataCenterId(@Param("dataCenterId") Long dataCenterId);

	List<DataCenterHistory> findByDataCenterIdentity_DataCenterIdOrderByDataCenterHistoryIdAsc(Long dataCenterId);

	@Query("""
			select count(d) > 0 from DataCenterHistory d
			where d.validTo is null and d.status = 'Active'
			and lower(d.dataCenterName) = lower(:name)
			and (:excludeId is null or d.dataCenterIdentity.dataCenterId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);

	@Query(value = """
			select distinct c.CAGE_ID
			from T_CAGE_HISTORY c
			where c.VALID_TO is null and c.STATUS = 'Active'
			and c.DATA_CENTER_ID = :dataCenterId
			""", nativeQuery = true)
	List<Long> findActiveCageIdsForDataCenter(@Param("dataCenterId") Long dataCenterId);
}
