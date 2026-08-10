package com.dcim.connectivity.crossconnecttype;

import java.time.Instant;
import java.time.LocalDate;

public record CrossConnectTypeDto(
		Long crossConnectTypeId,
		Long crossConnectTypeHistoryId,
		String crossConnectTypeName,
		Long chargeTypeId,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static CrossConnectTypeDto from(CrossConnectTypeHistory history) {
		return new CrossConnectTypeDto(
				history.getCrossConnectTypeId(),
				history.getCrossConnectTypeHistoryId(),
				history.getCrossConnectTypeName(),
				history.getChargeTypeId(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
