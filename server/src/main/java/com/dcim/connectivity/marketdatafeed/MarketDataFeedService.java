package com.dcim.connectivity.marketdatafeed;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFeedService {

	private final MarketDataFeedHistoryRepository feeds;

	MarketDataFeedService(MarketDataFeedHistoryRepository feeds) {
		this.feeds = feeds;
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> listCurrent() {
		return feeds.findCurrent().stream().map(MarketDataFeedDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> listCurrentByCrossConnect(Long crossConnectId) {
		return feeds.findCurrentByCrossConnectId(crossConnectId).stream().map(MarketDataFeedDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<MarketDataFeedDto> findCurrent(Long marketDataFeedId) {
		return feeds.findCurrentByMarketDataFeedId(marketDataFeedId).map(MarketDataFeedDto::from);
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> history(Long marketDataFeedId) {
		return feeds.findByMarketDataFeedIdentity_MarketDataFeedIdOrderByMarketDataFeedHistoryIdAsc(marketDataFeedId)
				.stream()
				.map(MarketDataFeedDto::from)
				.toList();
	}
}
