package com.dcim.site.rackdevicetype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class RackDeviceTypeServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesRackDeviceTypeThroughChangeWorkflow() {
		Long id = applyAdd(
				AssetType.RACK_DEVICE_TYPE,
				json(Map.of(
						"rackDeviceTypeName", unique("Switch"),
						"rackDeviceTypeKind", "EXTRANET_SWITCH")))
				.assetIdentityId();

		assertThat(rackDeviceTypes.findCurrent(id).orElseThrow().rackDeviceTypeKind())
				.isEqualTo(RackDeviceTypeKind.EXTRANET_SWITCH);
		assertThat(rackDeviceTypes.listCurrent()).extracting(RackDeviceTypeDto::rackDeviceTypeId).contains(id);

		applyUpdateCurrent(
				AssetType.RACK_DEVICE_TYPE,
				id,
				json(Map.of(
						"rackDeviceTypeName", unique("Patch"),
						"rackDeviceTypeKind", "PATCH_PANEL")));
		assertThat(rackDeviceTypes.findCurrent(id).orElseThrow().rackDeviceTypeKind())
				.isEqualTo(RackDeviceTypeKind.PATCH_PANEL);
		assertThat(rackDeviceTypes.history(id)).hasSize(2);

		applyTerminateCurrent(AssetType.RACK_DEVICE_TYPE, id);
		assertThat(rackDeviceTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(rackDeviceTypes.history(id)).hasSize(3);
	}
}
