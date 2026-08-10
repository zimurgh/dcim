package com.dcim.connectivity.cable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class CableServiceTests extends ChangeTestSupport {

	@Test
	void addsCableThroughChangeWorkflow() {
		CableSeed seed = seedCableDeps();
		ChangeDto applied = applyAdd(
				AssetType.CABLE,
				json(Map.of(
						"cableName", "CBL-1",
						"portAId", seed.portAId(),
						"portBId", seed.portBId(),
						"crossConnectId", seed.crossConnectId())));

		CableDto current = cables.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.cableName()).isEqualTo("CBL-1");
		assertThat(current.portAId()).isEqualTo(seed.portAId());
		assertThat(current.portBId()).isEqualTo(seed.portBId());
		assertThat(current.crossConnectId()).isEqualTo(seed.crossConnectId());
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(cables.listCurrentByCrossConnect(seed.crossConnectId())).hasSize(1);
		assertThat(cables.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesCableThroughChangeWorkflow() {
		CableSeed seed = seedCableDeps();
		Long sparePortId = seedRackDevicePort(unique("eth"), seed.deviceId(), seed.portTypeId());

		ChangeDto added = applyAdd(
				AssetType.CABLE,
				json(Map.of(
						"cableName", "CBL-1",
						"portAId", seed.portAId(),
						"portBId", seed.portBId(),
						"crossConnectId", seed.crossConnectId())));

		applyUpdateCurrent(
				AssetType.CABLE,
				added.assetIdentityId(),
				json(fields(
						"cableName", "CBL-1B",
						"portAId", seed.portAId(),
						"portBId", sparePortId,
						"crossConnectId", null)));

		CableDto current = cables.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.cableName()).isEqualTo("CBL-1B");
		assertThat(current.portBId()).isEqualTo(sparePortId);
		assertThat(current.crossConnectId()).isNull();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(cables.listCurrentByCrossConnect(seed.crossConnectId())).isEmpty();
		assertThat(cables.history(added.assetIdentityId())).hasSize(2);
		assertThat(cables.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesCableThroughChangeWorkflow() {
		CableSeed seed = seedCableDeps();
		ChangeDto added = applyAdd(
				AssetType.CABLE,
				json(Map.of(
						"cableName", "CBL-1",
						"portAId", seed.portAId(),
						"portBId", seed.portBId(),
						"crossConnectId", seed.crossConnectId())));

		applyTerminateCurrent(AssetType.CABLE, added.assetIdentityId());

		CableDto current = cables.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.cableName()).isEqualTo("CBL-1");
		assertThat(cables.history(added.assetIdentityId())).hasSize(2);
		assertThat(cables.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private CableSeed seedCableDeps() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long portAId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		Long portBId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);
		assertThat(rackDevicePorts.findCurrent(portAId)).isPresent();
		return new CableSeed(crossConnectId, device.rackDeviceId(), portTypeId, portAId, portBId);
	}

	private record CableSeed(
			Long crossConnectId,
			Long deviceId,
			Long portTypeId,
			Long portAId,
			Long portBId) {
	}
}
