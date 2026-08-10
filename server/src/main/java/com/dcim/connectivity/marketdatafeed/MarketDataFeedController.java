package com.dcim.connectivity.marketdatafeed;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/market-data-feeds")
class MarketDataFeedController {

	private final MarketDataFeedService feeds;

	MarketDataFeedController(MarketDataFeedService feeds) {
		this.feeds = feeds;
	}

	@GetMapping
	List<MarketDataFeedDto> list(@RequestParam(required = false) Long crossConnectId) {
		if (crossConnectId != null) {
			return feeds.listCurrentByCrossConnect(crossConnectId);
		}
		return feeds.listCurrent();
	}

	@GetMapping("/{marketDataFeedId}")
	MarketDataFeedDto get(@PathVariable Long marketDataFeedId) {
		return AssetHttp.requireFound(feeds.findCurrent(marketDataFeedId), "Market data feed", marketDataFeedId);
	}

	@GetMapping("/{marketDataFeedId}/history")
	List<MarketDataFeedDto> history(@PathVariable Long marketDataFeedId) {
		return AssetHttp.requireNonEmpty(feeds.history(marketDataFeedId), "Market data feed", marketDataFeedId);
	}
}
