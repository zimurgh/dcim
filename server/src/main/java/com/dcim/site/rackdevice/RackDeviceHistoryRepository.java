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

	@Query("""
			select count(d) > 0 from RackDeviceHistory d
			where d.validTo is null and d.status = 'Active'
			and lower(d.rackDeviceName) = lower(:name)
			and d.rackIdentity.rackId = :rackId
			and (:excludeId is null or d.rackDeviceIdentity.rackDeviceId <> :excludeId)
			""")
	boolean existsActiveNameClashInRack(
			@Param("name") String name, @Param("rackId") Long rackId, @Param("excludeId") Long excludeId);

	@Query(value = """
			select count(*)
			from T_RACK_HISTORY r
			where r.VALID_TO is null and r.STATUS = 'Active'
			and r.RACK_ID = :rackId
			""", nativeQuery = true)
	long countActiveRacks(@Param("rackId") Long rackId);

	@Query(value = """
			select count(*)
			from T_RACK_DEVICE_TYPE_HISTORY t
			where t.VALID_TO is null and t.STATUS = 'Active'
			and t.RACK_DEVICE_TYPE_ID = :rackDeviceTypeId
			""", nativeQuery = true)
	long countActiveRackDeviceTypes(@Param("rackDeviceTypeId") Long rackDeviceTypeId);

	@Query(value = """
			select distinct p.RACK_DEVICE_PORT_ID
			from T_RACK_DEVICE_PORT_HISTORY p
			where p.VALID_TO is null and p.STATUS = 'Active'
			and p.RACK_DEVICE_ID = :rackDeviceId
			""", nativeQuery = true)
	List<Long> findActiveRackDevicePortIdsForDevice(@Param("rackDeviceId") Long rackDeviceId);
}
