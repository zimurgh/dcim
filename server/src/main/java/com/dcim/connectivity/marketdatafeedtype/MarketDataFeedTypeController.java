package com.dcim.connectivity.marketdatafeedtype;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
		return types.findCurrent(marketDataFeedTypeId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Market data feed type not found: " + marketDataFeedTypeId));
	}

	@GetMapping("/{marketDataFeedTypeId}/history")
	List<MarketDataFeedTypeDto> history(@PathVariable Long marketDataFeedTypeId) {
		List<MarketDataFeedTypeDto> rows = types.history(marketDataFeedTypeId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Market data feed type not found: " + marketDataFeedTypeId);
		}
		return rows;
	}
}
