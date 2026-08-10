package com.dcim.organization.marketsegment;

import com.dcim.asset.AuditSlice;
import com.dcim.asset.AuditedDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record MarketSegmentDto(
		Long marketSegmentId,
		Long marketSegmentHistoryId,
		String marketSegmentName,
		MarketSegmentType marketSegmentType,
		@JsonUnwrapped AuditSlice audit) implements AuditedDto {

	static MarketSegmentDto from(MarketSegmentHistory history) {
		return new MarketSegmentDto(
				history.getMarketSegmentId(),
				history.getMarketSegmentHistoryId(),
				history.getMarketSegmentName(),
				history.getMarketSegmentType(),
				AuditSlice.from(history));
	}
}
