package com.dcim.organization.marketsegment;

import java.time.Instant;
import java.time.LocalDate;

public record MarketSegmentDto(
		Long marketSegmentId,
		Long marketSegmentHistoryId,
		String marketSegmentName,
		MarketSegmentType marketSegmentType,
		LocalDate validFrom,
		LocalDate validTo,
		Instant appliedAt,
		Long appliedBy,
		String action,
		String status) {

	static MarketSegmentDto from(MarketSegmentHistory history) {
		return new MarketSegmentDto(
				history.getMarketSegmentId(),
				history.getMarketSegmentHistoryId(),
				history.getMarketSegmentName(),
				history.getMarketSegmentType(),
				history.getValidFrom(),
				history.getValidTo(),
				history.getAppliedAt(),
				history.getAppliedBy(),
				history.getAction(),
				history.getStatus());
	}
}
