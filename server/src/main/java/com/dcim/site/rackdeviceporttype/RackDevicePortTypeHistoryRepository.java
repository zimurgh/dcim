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
}
