package com.dcim.connectivity.speed;

import java.time.Instant;
import java.time.LocalDate;

public record SpeedDto(
		Long speedId,
		Long speedHistoryId,
		String speedName,
		SpeedType speedType,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static SpeedDto from(SpeedHistory history) {
		return new SpeedDto(
				history.getSpeedId(),
				history.getSpeedHistoryId(),
				history.getSpeedName(),
				history.getSpeedType(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
