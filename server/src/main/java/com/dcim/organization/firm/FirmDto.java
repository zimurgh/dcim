package com.dcim.organization.firm;

import java.time.Instant;
import java.time.LocalDate;

public record FirmDto(
		Long firmId,
		Long firmHistoryId,
		String firmName,
		String parentFirmName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		String appliedBy,
		String action,
		String status) {

	static FirmDto from(FirmHistory history) {
		return new FirmDto(
				history.getFirmId(),
				history.getFirmHistoryId(),
				history.getFirmName(),
				history.getParentFirmName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
