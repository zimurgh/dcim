package com.dcim.site.rackdeviceport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackDevicePortHistoryRepository extends JpaRepository<RackDevicePortHistory, Long> {

	@Query("""
			select p from RackDevicePortHistory p
			where p.validTo is null
			order by p.rackDevicePortName
			""")
	List<RackDevicePortHistory> findCurrentRackDevicePorts();

	@Query("""
			select p from RackDevicePortHistory p
			where p.rackDevicePortIdentity.rackDevicePortId = :rackDevicePortId and p.validTo is null
			""")
	Optional<RackDevicePortHistory> findCurrentByRackDevicePortId(@Param("rackDevicePortId") Long rackDevicePortId);

	@Query("""
			select p from RackDevicePortHistory p
			where p.rackDeviceIdentity.rackDeviceId = :rackDeviceId and p.validTo is null
			order by p.rackDevicePortName
			""")
	List<RackDevicePortHistory> findCurrentByRackDeviceId(@Param("rackDeviceId") Long rackDeviceId);

	List<RackDevicePortHistory> findByRackDevicePortIdentity_RackDevicePortIdOrderByRackDevicePortHistoryIdAsc(
			Long rackDevicePortId);
}
