package com.dcim.connectivity.latency;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dcim.asset.AssetHttp;

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
		return AssetHttp.requireFound(latencies.findCurrent(latencyId), "Latency", latencyId);
	}

	@GetMapping("/{latencyId}/history")
	List<LatencyDto> history(@PathVariable Long latencyId) {
		return AssetHttp.requireNonEmpty(latencies.history(latencyId), "Latency", latencyId);
	}
}
