package com.dcim.connectivity.chargetype;

import java.time.Instant;
import java.time.LocalDate;

public record ChargeTypeDto(
		Long chargeTypeId,
		Long chargeTypeHistoryId,
		String chargeTypeName,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static ChargeTypeDto from(ChargeTypeHistory history) {
		return new ChargeTypeDto(
				history.getChargeTypeId(),
				history.getChargeTypeHistoryId(),
				history.getChargeTypeName(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
