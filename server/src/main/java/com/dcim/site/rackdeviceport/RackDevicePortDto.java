package com.dcim.site.rackdeviceport;

import java.time.Instant;
import java.time.LocalDate;

public record RackDevicePortDto(
		Long rackDevicePortId,
		Long rackDevicePortHistoryId,
		Long rackDeviceId,
		Long rackDevicePortTypeId,
		String rackDevicePortName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static RackDevicePortDto from(RackDevicePortHistory history) {
		return new RackDevicePortDto(
				history.getRackDevicePortId(),
				history.getRackDevicePortHistoryId(),
				history.getRackDeviceId(),
				history.getRackDevicePortTypeId(),
				history.getRackDevicePortName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
