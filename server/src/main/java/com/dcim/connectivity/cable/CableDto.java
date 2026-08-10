package com.dcim.connectivity.cable;

import java.time.Instant;
import java.time.LocalDate;

public record CableDto(
		Long cableId,
		Long cableHistoryId,
		String cableName,
		Long portAId,
		Long portBId,
		Long crossConnectId,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static CableDto from(CableHistory history) {
		return new CableDto(
				history.getCableId(),
				history.getCableHistoryId(),
				history.getCableName(),
				history.getPortAId(),
				history.getPortBId(),
				history.getCrossConnectId(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
