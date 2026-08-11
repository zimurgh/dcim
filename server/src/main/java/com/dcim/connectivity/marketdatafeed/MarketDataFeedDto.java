package com.dcim.connectivity.marketdatafeed;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MarketDataFeedDto(
		Long marketDataFeedId,
		Long marketDataFeedHistoryId,
		Long crossConnectId,
		String crossConnectName,
		Long marketDataFeedTypeId,
		String marketDataFeedTypeName,
		String marketDataFeedName,
		Long ownerFirmId,
		String ownerFirmName,
		Long billingFirmId,
		String billingFirmName,
		Long providerFirmId,
		String providerFirmName,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static MarketDataFeedDto from(MarketDataFeedHistory history) {
		return new MarketDataFeedDto(
				history.getMarketDataFeedId(),
				history.getMarketDataFeedHistoryId(),
				history.getCrossConnectId(),
				null,
				history.getMarketDataFeedTypeId(),
				null,
				history.getMarketDataFeedName(),
				history.getOwnerFirmId(),
				null,
				history.getBillingFirmId(),
				null,
				history.getProviderFirmId(),
				null,
				AuditSlice.from(history));
	}

	static MarketDataFeedDto from(MarketDataFeedView view) {
		return new MarketDataFeedDto(
				view.getMarketDataFeedId(),
				view.getMarketDataFeedHistoryId(),
				view.getCrossConnectId(),
				view.getCrossConnectName(),
				view.getMarketDataFeedTypeId(),
				view.getMarketDataFeedTypeName(),
				view.getMarketDataFeedName(),
				view.getOwnerFirmId(),
				view.getOwnerFirmName(),
				view.getBillingFirmId(),
				view.getBillingFirmName(),
				view.getProviderFirmId(),
				view.getProviderFirmName(),
				AuditSlice.from(view));
	}
}
