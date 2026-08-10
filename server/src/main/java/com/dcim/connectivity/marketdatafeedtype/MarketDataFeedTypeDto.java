package com.dcim.connectivity.marketdatafeedtype;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MarketDataFeedTypeDto(
		Long marketDataFeedTypeId,
		Long marketDataFeedTypeHistoryId,
		String marketDataFeedTypeName,
		Long chargeTypeId,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static MarketDataFeedTypeDto from(MarketDataFeedTypeHistory history) {
		return new MarketDataFeedTypeDto(
				history.getMarketDataFeedTypeId(),
				history.getMarketDataFeedTypeHistoryId(),
				history.getMarketDataFeedTypeName(),
				history.getChargeTypeId(),
				AuditSlice.from(history));
	}
}
