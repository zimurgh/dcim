package com.dcim.connectivity.latency;

import java.time.Instant;
import java.time.LocalDate;

public record LatencyDto(
		Long latencyId,
		Long latencyHistoryId,
		String latencyName,
		LatencyType latencyType,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static LatencyDto from(LatencyHistory history) {
		return new LatencyDto(
				history.getLatencyId(),
				history.getLatencyHistoryId(),
				history.getLatencyName(),
				history.getLatencyType(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
