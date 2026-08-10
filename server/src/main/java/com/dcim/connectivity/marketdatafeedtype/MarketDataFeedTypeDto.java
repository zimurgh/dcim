package com.dcim.connectivity.marketdatafeedtype;

import java.time.Instant;
import java.time.LocalDate;

public record MarketDataFeedTypeDto(
		Long marketDataFeedTypeId,
		Long marketDataFeedTypeHistoryId,
		String marketDataFeedTypeName,
		Long chargeTypeId,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static MarketDataFeedTypeDto from(MarketDataFeedTypeHistory history) {
		return new MarketDataFeedTypeDto(
				history.getMarketDataFeedTypeId(),
				history.getMarketDataFeedTypeHistoryId(),
				history.getMarketDataFeedTypeName(),
				history.getChargeTypeId(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
