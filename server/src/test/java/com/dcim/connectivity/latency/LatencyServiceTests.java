package com.dcim.connectivity.latency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class LatencyServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesLatencyThroughChangeWorkflow() {
		Long latencyId = applyAdd(
				AssetType.LATENCY,
				json(Map.of("latencyName", unique("Latency"), "latencyType", "LL")))
				.assetIdentityId();

		LatencyDto current = latencies.findCurrent(latencyId).orElseThrow();
		assertThat(current.latencyType()).isEqualTo(LatencyType.LL);
		assertThat(current.status()).isEqualTo("Active");
		assertThat(latencies.listCurrent()).extracting(LatencyDto::latencyId).contains(latencyId);
		assertThat(latencies.history(latencyId)).hasSize(1);

		applyUpdateCurrent(
				AssetType.LATENCY,
				latencyId,
				json(Map.of("latencyName", unique("Latency"), "latencyType", "ULL")));
		assertThat(latencies.findCurrent(latencyId).orElseThrow().latencyType()).isEqualTo(LatencyType.ULL);
		assertThat(latencies.history(latencyId)).hasSize(2);

		applyTerminateCurrent(AssetType.LATENCY, latencyId);
		assertThat(latencies.findCurrent(latencyId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(latencies.history(latencyId)).hasSize(3);
	}
}
