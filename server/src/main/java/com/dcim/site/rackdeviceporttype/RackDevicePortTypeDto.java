package com.dcim.site.rackdeviceporttype;

import java.time.Instant;
import java.time.LocalDate;

public record RackDevicePortTypeDto(
		Long rackDevicePortTypeId,
		Long rackDevicePortTypeHistoryId,
		String rackDevicePortTypeName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static RackDevicePortTypeDto from(RackDevicePortTypeHistory history) {
		return new RackDevicePortTypeDto(
				history.getRackDevicePortTypeId(),
				history.getRackDevicePortTypeHistoryId(),
				history.getRackDevicePortTypeName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
