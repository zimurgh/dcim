package com.dcim.site.rackdevice;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackDeviceHistoryRepository extends JpaRepository<RackDeviceHistory, Long> {

	@Query("""
			select d from RackDeviceHistory d
			where d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceHistory> findCurrentRackDevices();

	@Query("""
			select d from RackDeviceHistory d
			where d.rackDeviceIdentity.rackDeviceId = :rackDeviceId and d.validTo is null
			""")
	Optional<RackDeviceHistory> findCurrentByRackDeviceId(@Param("rackDeviceId") Long rackDeviceId);

	@Query("""
			select d from RackDeviceHistory d
			where d.rackIdentity.rackId = :rackId and d.validTo is null
			order by d.rackDeviceName
			""")
	List<RackDeviceHistory> findCurrentByRackId(@Param("rackId") Long rackId);

	List<RackDeviceHistory> findByRackDeviceIdentity_RackDeviceIdOrderByRackDeviceHistoryIdAsc(Long rackDeviceId);
}
