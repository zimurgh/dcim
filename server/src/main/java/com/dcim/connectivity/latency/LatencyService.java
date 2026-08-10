package com.dcim.connectivity.latency;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LatencyService {

	private final LatencyHistoryRepository latencies;

	LatencyService(LatencyHistoryRepository latencies) {
		this.latencies = latencies;
	}

	@Transactional(readOnly = true)
	public List<LatencyDto> listCurrent() {
		return latencies.findCurrent().stream().map(LatencyDto::from).toList();
	}

	@Transactional(readOnly = true)
	public Optional<LatencyDto> findCurrent(Long latencyId) {
		return latencies.findCurrentByLatencyId(latencyId).map(LatencyDto::from);
	}

	@Transactional(readOnly = true)
	public List<LatencyDto> history(Long latencyId) {
		return latencies.findByLatencyIdentity_LatencyIdOrderByLatencyHistoryIdAsc(latencyId).stream()
				.map(LatencyDto::from)
				.toList();
	}
}
