package com.dcim.site.rackdevice;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Shallow rack device DTO for API responses (current history row).
 */
public record RackDeviceDto(
		Long rackDeviceId,
		Long rackDeviceHistoryId,
		Long rackId,
		String rackDeviceName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		String appliedBy,
		String action,
		String status) {

	static RackDeviceDto from(RackDeviceHistory history) {
		return new RackDeviceDto(
				history.getRackDeviceId(),
				history.getRackDeviceHistoryId(),
				history.getRackId(),
				history.getRackDeviceName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
