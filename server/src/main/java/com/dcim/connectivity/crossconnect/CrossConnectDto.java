package com.dcim.connectivity.crossconnect;

import java.time.Instant;
import java.time.LocalDate;

public record CrossConnectDto(
		Long crossConnectId,
		Long crossConnectHistoryId,
		String crossConnectName,
		String circuitId,
		Long crossConnectTypeId,
		Long latencyId,
		Long speedId,
		Long marketSegmentId,
		Long ownerFirmId,
		Long billingFirmId,
		Long providerFirmId,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static CrossConnectDto from(CrossConnectHistory history) {
		return new CrossConnectDto(
				history.getCrossConnectId(),
				history.getCrossConnectHistoryId(),
				history.getCrossConnectName(),
				history.getCircuitId(),
				history.getCrossConnectTypeId(),
				history.getLatencyId(),
				history.getSpeedId(),
				history.getMarketSegmentId(),
				history.getOwnerFirmId(),
				history.getBillingFirmId(),
				history.getProviderFirmId(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
