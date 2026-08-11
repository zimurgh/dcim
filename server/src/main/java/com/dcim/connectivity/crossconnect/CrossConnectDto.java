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
		String crossConnectTypeName,
		Long latencyId,
		String latencyName,
		Long speedId,
		String speedName,
		Long marketSegmentId,
		String marketSegmentName,
		Long ownerFirmId,
		String ownerFirmName,
		Long billingFirmId,
		String billingFirmName,
		Long providerFirmId,
		String providerFirmName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static CrossConnectDto from(CrossConnectHistory history) {
		return new CrossConnectDto(
				history.getCrossConnectId(),
				history.getCrossConnectHistoryId(),
				history.getCrossConnectName(),
				history.getCircuitId(),
				history.getCrossConnectTypeId(),
				null,
				history.getLatencyId(),
				null,
				history.getSpeedId(),
				null,
				history.getMarketSegmentId(),
				null,
				history.getOwnerFirmId(),
				null,
				history.getBillingFirmId(),
				null,
				history.getProviderFirmId(),
				null,
				AuditSlice.from(history));
	}

	static CrossConnectDto from(CrossConnectView view) {
		return new CrossConnectDto(
				view.getCrossConnectId(),
				view.getCrossConnectHistoryId(),
				view.getCrossConnectName(),
				view.getCircuitId(),
				view.getCrossConnectTypeId(),
				view.getCrossConnectTypeName(),
				view.getLatencyId(),
				view.getLatencyName(),
				view.getSpeedId(),
				view.getSpeedName(),
				view.getMarketSegmentId(),
				view.getMarketSegmentName(),
				view.getOwnerFirmId(),
				view.getOwnerFirmName(),
				view.getBillingFirmId(),
				view.getBillingFirmName(),
				view.getProviderFirmId(),
				view.getProviderFirmName(),
				AuditSlice.from(view));
	}
}
