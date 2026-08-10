package com.dcim.connectivity.latency;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/latencies")
class LatencyController {

	private final LatencyService latencies;

	LatencyController(LatencyService latencies) {
		this.latencies = latencies;
	}

	@GetMapping
	List<LatencyDto> list() {
		return latencies.listCurrent();
	}

	@GetMapping("/{latencyId}")
	LatencyDto get(@PathVariable Long latencyId) {
		return latencies.findCurrent(latencyId)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Latency not found: " + latencyId));
	}

	@GetMapping("/{latencyId}/history")
	List<LatencyDto> history(@PathVariable Long latencyId) {
		List<LatencyDto> rows = latencies.history(latencyId);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Latency not found: " + latencyId);
		}
		return rows;
	}
}
