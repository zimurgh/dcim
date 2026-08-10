package com.dcim.site.rackdevice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackDeviceViewRepository extends JpaRepository<RackDeviceView, Long> {

	@Query("""
			select d from RackDeviceView d
			where d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceView> findCurrentRackDevices();

	@Query("""
			select d from RackDeviceView d
			where d.rackDeviceIdentity.rackDeviceId = :rackDeviceId and d.validTo is null
			""")
	Optional<RackDeviceView> findCurrentByRackDeviceId(@Param("rackDeviceId") Long rackDeviceId);

	@Query("""
			select d from RackDeviceView d
			where d.rackId = :rackId and d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceView> findCurrentByRackId(@Param("rackId") Long rackId);

	@Query("""
			select d from RackDeviceView d
			where d.cageId = :cageId and d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceView> findCurrentByCageId(@Param("cageId") Long cageId);

	@Query("""
			select d from RackDeviceView d
			where d.dataCenterId = :dataCenterId and d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceView> findCurrentByDataCenterId(@Param("dataCenterId") Long dataCenterId);

	List<RackDeviceView> findByRackDeviceIdentity_RackDeviceIdOrderByRackDeviceHistoryIdAsc(Long rackDeviceId);
}
