package com.dcim.connectivity.speed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class SpeedServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesSpeedThroughChangeWorkflow() {
		Long speedId = applyAdd(
				AssetType.SPEED,
				json(Map.of("speedName", unique("Speed"), "speedType", "1G")))
				.assetIdentityId();

		SpeedDto current = speeds.findCurrent(speedId).orElseThrow();
		assertThat(current.speedType()).isEqualTo(SpeedType.ONE_G);
		assertThat(speeds.listCurrent()).extracting(SpeedDto::speedId).contains(speedId);

		applyUpdateCurrent(
				AssetType.SPEED,
				speedId,
				json(Map.of("speedName", unique("Speed"), "speedType", "10G")));
		assertThat(speeds.findCurrent(speedId).orElseThrow().speedType()).isEqualTo(SpeedType.TEN_G);
		assertThat(speeds.history(speedId)).hasSize(2);

		applyTerminateCurrent(AssetType.SPEED, speedId);
		assertThat(speeds.findCurrent(speedId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(speeds.history(speedId)).hasSize(3);
	}
}
