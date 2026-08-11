package com.dcim.site.rackdeviceporttype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class RackDevicePortTypeServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesRackDevicePortTypeThroughChangeWorkflow() {
		Long id = applyAdd(
				AssetType.RACK_DEVICE_PORT_TYPE,
				json(Map.of("rackDevicePortTypeName", unique("RJ45"))))
				.assetIdentityId();

		assertThat(rackDevicePortTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Active");
		assertThat(rackDevicePortTypes.listCurrent()).extracting(RackDevicePortTypeDto::rackDevicePortTypeId)
				.contains(id);

		applyUpdateCurrent(
				AssetType.RACK_DEVICE_PORT_TYPE,
				id,
				json(Map.of("rackDevicePortTypeName", unique("LC"))));
		assertThat(rackDevicePortTypes.history(id)).hasSize(2);

		applyTerminateCurrent(AssetType.RACK_DEVICE_PORT_TYPE, id);
		assertThat(rackDevicePortTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(rackDevicePortTypes.history(id)).hasSize(3);
	}
}
