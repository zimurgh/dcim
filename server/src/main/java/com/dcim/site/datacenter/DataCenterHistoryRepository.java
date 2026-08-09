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
}
