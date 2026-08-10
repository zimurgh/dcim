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
}
