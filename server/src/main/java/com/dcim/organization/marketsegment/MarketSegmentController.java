package com.dcim.organization.marketsegment;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

@RestController
@RequestMapping("/api/market-segments")
class MarketSegmentController {

	private final MarketSegmentService marketSegments;

	MarketSegmentController(MarketSegmentService marketSegments) {
		this.marketSegments = marketSegments;
	}

	@GetMapping
	List<MarketSegmentDto> list() {
		return marketSegments.listCurrent();
	}

	@GetMapping("/{marketSegmentId}")
	MarketSegmentDto get(@PathVariable Long marketSegmentId) {
		return AssetHttp.requireFound(marketSegments.findCurrent(marketSegmentId), "Market segment", marketSegmentId);
	}

	@GetMapping("/{marketSegmentId}/history")
	List<MarketSegmentDto> history(@PathVariable Long marketSegmentId) {
		return AssetHttp.requireNonEmpty(marketSegments.history(marketSegmentId), "Market segment", marketSegmentId);
	}
}
