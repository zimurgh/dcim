package com.dcim.site.rackdevicetype;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RackDeviceTypeHistoryRepository extends JpaRepository<RackDeviceTypeHistory, Long> {

	@Query("""
			select t from RackDeviceTypeHistory t
			where t.validTo is null
			order by t.rackDeviceTypeName
			""")
	List<RackDeviceTypeHistory> findCurrent();

	@Query("""
			select t from RackDeviceTypeHistory t
			where t.rackDeviceTypeIdentity.rackDeviceTypeId = :rackDeviceTypeId and t.validTo is null
			""")
	Optional<RackDeviceTypeHistory> findCurrentByRackDeviceTypeId(
			@Param("rackDeviceTypeId") Long rackDeviceTypeId);

	List<RackDeviceTypeHistory> findByRackDeviceTypeIdentity_RackDeviceTypeIdOrderByRackDeviceTypeHistoryIdAsc(
			Long rackDeviceTypeId);

	@Query("""
			select count(t) > 0 from RackDeviceTypeHistory t
			where t.validTo is null and t.status = 'Active'
			and lower(t.rackDeviceTypeName) = lower(:name)
			and (:excludeId is null or t.rackDeviceTypeIdentity.rackDeviceTypeId <> :excludeId)
			""")
	boolean existsActiveNameClash(@Param("name") String name, @Param("excludeId") Long excludeId);

	@Query(value = """
			select distinct d.RACK_DEVICE_ID
			from T_RACK_DEVICE_HISTORY d
			where d.VALID_TO is null and d.STATUS = 'Active'
			and d.RACK_DEVICE_TYPE_ID = :rackDeviceTypeId
			""", nativeQuery = true)
	List<Long> findActiveRackDeviceIdsForType(@Param("rackDeviceTypeId") Long rackDeviceTypeId);
}
