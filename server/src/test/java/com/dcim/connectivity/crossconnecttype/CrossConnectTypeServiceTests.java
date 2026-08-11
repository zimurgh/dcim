package com.dcim.connectivity.crossconnecttype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class CrossConnectTypeServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesCrossConnectTypeThroughChangeWorkflow() {
		Long id = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				json(Map.of("crossConnectTypeName", unique("Type"))))
				.assetIdentityId();

		assertThat(crossConnectTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Active");
		assertThat(crossConnectTypes.listCurrent()).extracting(CrossConnectTypeDto::crossConnectTypeId)
				.contains(id);

		applyUpdateCurrent(
				AssetType.CROSS_CONNECT_TYPE,
				id,
				json(Map.of("crossConnectTypeName", unique("Type-B"))));
		assertThat(crossConnectTypes.history(id)).hasSize(2);

		applyTerminateCurrent(AssetType.CROSS_CONNECT_TYPE, id);
		assertThat(crossConnectTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(crossConnectTypes.history(id)).hasSize(3);
	}
}
