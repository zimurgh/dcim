package com.dcim.site.rackdevicetype;

import java.time.Instant;
import java.time.LocalDate;

public record RackDeviceTypeDto(
		Long rackDeviceTypeId,
		Long rackDeviceTypeHistoryId,
		String rackDeviceTypeName,
		RackDeviceTypeKind rackDeviceTypeKind,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static RackDeviceTypeDto from(RackDeviceTypeHistory history) {
		return new RackDeviceTypeDto(
				history.getRackDeviceTypeId(),
				history.getRackDeviceTypeHistoryId(),
				history.getRackDeviceTypeName(),
				history.getRackDeviceTypeKind(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
