package com.dcim.asset;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Audit/temporal fields shared by every asset history DTO.
 */
public record AuditSlice(
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	public static AuditSlice from(AuditHistory history) {
		return new AuditSlice(
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
