package com.dcim.site.cage;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Shallow cage DTO for API responses (current history row).
 */
public record CageDto(
		Long cageId,
		Long cageHistoryId,
		Long dataCenterId,
		String cageName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static CageDto from(CageHistory history) {
		return new CageDto(
				history.getCageId(),
				history.getCageHistoryId(),
				history.getDataCenterId(),
				history.getCageName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
