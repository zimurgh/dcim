package com.dcim.connectivity.crossconnect;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

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
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

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
				AuditSlice.from(history));
	}
}
