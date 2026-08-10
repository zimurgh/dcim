package com.dcim.connectivity.marketdatafeedtype;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/market-data-feed-types")
class MarketDataFeedTypeController {

	private final MarketDataFeedTypeService types;

	MarketDataFeedTypeController(MarketDataFeedTypeService types) {
		this.types = types;
	}

	@GetMapping
	List<MarketDataFeedTypeDto> list() {
		return types.listCurrent();
	}

	@GetMapping("/{marketDataFeedTypeId}")
	MarketDataFeedTypeDto get(@PathVariable Long marketDataFeedTypeId) {
		return AssetHttp.requireFound(types.findCurrent(marketDataFeedTypeId), "Market data feed type", marketDataFeedTypeId);
	}

	@GetMapping("/{marketDataFeedTypeId}/history")
	List<MarketDataFeedTypeDto> history(@PathVariable Long marketDataFeedTypeId) {
		return AssetHttp.requireNonEmpty(types.history(marketDataFeedTypeId), "Market data feed type", marketDataFeedTypeId);
	}
}
