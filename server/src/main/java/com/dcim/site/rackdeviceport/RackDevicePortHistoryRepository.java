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

	@Query("""
			select count(p) > 0 from RackDevicePortHistory p
			where p.validTo is null and p.status = 'Active'
			and lower(p.rackDevicePortName) = lower(:name)
			and p.rackDeviceIdentity.rackDeviceId = :rackDeviceId
			and (:excludeId is null or p.rackDevicePortIdentity.rackDevicePortId <> :excludeId)
			""")
	boolean existsActiveNameClashInDevice(
			@Param("name") String name, @Param("rackDeviceId") Long rackDeviceId, @Param("excludeId") Long excludeId);

	@Query(value = """
			select count(*)
			from T_RACK_DEVICE_HISTORY d
			where d.VALID_TO is null and d.STATUS = 'Active'
			and d.RACK_DEVICE_ID = :rackDeviceId
			""", nativeQuery = true)
	long countActiveRackDevices(@Param("rackDeviceId") Long rackDeviceId);

	@Query(value = """
			select count(*)
			from T_RACK_DEVICE_PORT_TYPE_HISTORY t
			where t.VALID_TO is null and t.STATUS = 'Active'
			and t.RACK_DEVICE_PORT_TYPE_ID = :rackDevicePortTypeId
			""", nativeQuery = true)
	long countActiveRackDevicePortTypes(@Param("rackDevicePortTypeId") Long rackDevicePortTypeId);

	@Query(value = """
			select distinct c.CABLE_ID
			from T_CABLE_HISTORY c
			where c.VALID_TO is null and c.STATUS = 'Active'
			and (c.PORT_A_ID = :rackDevicePortId or c.PORT_B_ID = :rackDevicePortId)
			""", nativeQuery = true)
	List<Long> findActiveCableIdsForPort(@Param("rackDevicePortId") Long rackDevicePortId);
}
