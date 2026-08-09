package com.dcim.site.rack;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Shallow rack DTO for API responses (current history row).
 */
public record RackDto(
		Long rackId,
		Long rackHistoryId,
		Long cageId,
		String rackName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		String appliedBy,
		String action,
		String status) {

	static RackDto from(RackHistory history) {
		return new RackDto(
				history.getRackId(),
				history.getRackHistoryId(),
				history.getCageId(),
				history.getRackName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
