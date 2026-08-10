package com.dcim.connectivity.marketdatafeedtype;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFeedTypeService {

	private final MarketDataFeedTypeHistoryRepository types;

	MarketDataFeedTypeService(MarketDataFeedTypeHistoryRepository types) {
		this.types = types;
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedTypeDto> listCurrent() {
		return types.findCurrent().stream().map(MarketDataFeedTypeDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<MarketDataFeedTypeDto> findCurrent(Long marketDataFeedTypeId) {
		return types.findCurrentByMarketDataFeedTypeId(marketDataFeedTypeId).map(MarketDataFeedTypeDto::from);
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedTypeDto> history(Long marketDataFeedTypeId) {
		return types
				.findByMarketDataFeedTypeIdentity_MarketDataFeedTypeIdOrderByMarketDataFeedTypeHistoryIdAsc(
						marketDataFeedTypeId)
				.stream()
				.map(MarketDataFeedTypeDto::from)
				.toList();
	}
}
