package com.dcim.organization.marketsegment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
		return marketSegments.findCurrent(marketSegmentId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Market segment not found: " + marketSegmentId));
	}

	@GetMapping("/{marketSegmentId}/history")
	List<MarketSegmentDto> history(@PathVariable Long marketSegmentId) {
		List<MarketSegmentDto> rows = marketSegments.history(marketSegmentId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND, "Market segment not found: " + marketSegmentId);
		}
		return rows;
	}
}
