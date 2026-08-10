package com.dcim.connectivity.marketdatafeed;

import java.time.Instant;
import java.time.LocalDate;

public record MarketDataFeedDto(
		Long marketDataFeedId,
		Long marketDataFeedHistoryId,
		Long crossConnectId,
		Long marketDataFeedTypeId,
		String marketDataFeedName,
		Long ownerFirmId,
		Long billingFirmId,
		Long providerFirmId,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

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
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
