package com.dcim.connectivity.marketdatafeed;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketDataFeedService {

	private final MarketDataFeedHistoryRepository feeds;
	private final MarketDataFeedViewRepository feedViews;

	MarketDataFeedService(MarketDataFeedHistoryRepository feeds, MarketDataFeedViewRepository feedViews) {
		this.feeds = feeds;
		this.feedViews = feedViews;
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> listCurrent() {
		return feedViews.findCurrent().stream().map(MarketDataFeedDto::from).toList();
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> listCurrentByCrossConnect(Long crossConnectId) {
		return feedViews.findCurrentByCrossConnectId(crossConnectId).stream().map(MarketDataFeedDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<MarketDataFeedDto> findCurrent(Long marketDataFeedId) {
		return feedViews.findCurrentByMarketDataFeedId(marketDataFeedId).map(MarketDataFeedDto::from);
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> history(Long marketDataFeedId) {
		return feeds.findByMarketDataFeedIdentity_MarketDataFeedIdOrderByMarketDataFeedHistoryIdAsc(marketDataFeedId)
				.stream()
				.map(MarketDataFeedDto::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MarketDataFeedDto> listCurrentByMarketDataFeedTypeId(Long marketDataFeedTypeId) {
		return feedViews.findCurrentByMarketDataFeedTypeId(marketDataFeedTypeId).stream()
				.map(MarketDataFeedDto::from)
				.toList();
	}
}
