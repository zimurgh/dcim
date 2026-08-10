package com.dcim.connectivity.marketdatafeed;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MarketDataFeedDto(
		Long marketDataFeedId,
		Long marketDataFeedHistoryId,
		Long crossConnectId,
		Long marketDataFeedTypeId,
		String marketDataFeedName,
		Long ownerFirmId,
		Long billingFirmId,
		Long providerFirmId,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static MarketDataFeedDto from(MarketDataFeedHistory history) {
		return new MarketDataFeedDto(
				history.getMarketDataFeedId(),
				history.getMarketDataFeedHistoryId(),
				history.getCrossConnectId(),
				history.getMarketDataFeedTypeId(),
				history.getMarketDataFeedName(),
				history.getOwnerFirmId(),
				history.getBillingFirmId(),
				history.getProviderFirmId(),
				AuditSlice.from(history));
	}
}
