package com.dcim.organization.marketsegment;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketSegmentService {

	private final MarketSegmentHistoryRepository marketSegments;

	MarketSegmentService(MarketSegmentHistoryRepository marketSegments) {
		this.marketSegments = marketSegments;
	}

	@Transactional(readOnly = true)
	public List<MarketSegmentDto> listCurrent() {
		return marketSegments.findCurrent().stream().map(MarketSegmentDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<MarketSegmentDto> findCurrent(Long marketSegmentId) {
		return marketSegments.findCurrentByMarketSegmentId(marketSegmentId).map(MarketSegmentDto::from);
	}

	@Transactional(readOnly = true)
	public List<MarketSegmentDto> history(Long marketSegmentId) {
		return marketSegments
				.findByMarketSegmentIdentity_MarketSegmentIdOrderByMarketSegmentHistoryIdAsc(marketSegmentId)
				.stream()
				.map(MarketSegmentDto::from)
				.toList();
	}
}
