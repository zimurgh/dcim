package com.dcim.connectivity.marketdatafeed;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
		return feeds.findCurrent(marketDataFeedId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Market data feed not found: " + marketDataFeedId));
	}

	@GetMapping("/{marketDataFeedId}/history")
	List<MarketDataFeedDto> history(@PathVariable Long marketDataFeedId) {
		List<MarketDataFeedDto> rows = feeds.history(marketDataFeedId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Market data feed not found: " + marketDataFeedId);
		}
		return rows;
	}
}
