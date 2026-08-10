package com.dcim.site.rackdeviceporttype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackDevicePortTypeHistoryRepository extends JpaRepository<RackDevicePortTypeHistory, Long> {

	@Query("""
			select t from RackDevicePortTypeHistory t
			where t.validTo is null
			order by t.rackDevicePortTypeName
			""")
	List<RackDevicePortTypeHistory> findCurrent();

	@Query("""
			select t from RackDevicePortTypeHistory t
			where t.rackDevicePortTypeIdentity.rackDevicePortTypeId = :rackDevicePortTypeId and t.validTo is null
			""")
	Optional<RackDevicePortTypeHistory> findCurrentByRackDevicePortTypeId(
			@Param("rackDevicePortTypeId") Long rackDevicePortTypeId);

	List<RackDevicePortTypeHistory>
			findByRackDevicePortTypeIdentity_RackDevicePortTypeIdOrderByRackDevicePortTypeHistoryIdAsc(
					Long rackDevicePortTypeId);

	@Query("""
			select count(t) > 0 from RackDevicePortTypeHistory t
			where t.validTo is null and t.status = 'Active'
			and lower(t.rackDevicePortTypeName) = lower(:name)
			and (:excludeId is null or t.rackDevicePortTypeIdentity.rackDevicePortTypeId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);

	@Query(value = """
			select distinct p.RACK_DEVICE_PORT_ID
			from T_RACK_DEVICE_PORT_HISTORY p
			where p.VALID_TO is null and p.STATUS = 'Active'
			and p.RACK_DEVICE_PORT_TYPE_ID = :rackDevicePortTypeId
			""", nativeQuery = true)
	List<Long> findActiveRackDevicePortIdsForType(@Param("rackDevicePortTypeId") Long rackDevicePortTypeId);
}
