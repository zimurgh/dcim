package com.dcim.site.rackdeviceport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.site.rackdevice.RackDeviceIdentity;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentity;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RackDevicePortServiceTests extends ChangeTestSupport {

	@Autowired
	RackDevicePortIdentityRepository portIdentities;

	@Autowired
	RackDevicePortHistoryRepository history;

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Autowired
	RackDevicePortTypeIdentityRepository portTypeIdentities;

	@Test
	void listsAndLoadsCurrentPortUnderRackDevice() {
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());
		RackDevicePortTypeIdentity portType = portTypeIdentities.save(new RackDevicePortTypeIdentity());
		RackDevicePortIdentity port = portIdentities.save(new RackDevicePortIdentity());

		history.save(new RackDevicePortHistory(
				port,
				device,
				portType,
				"eth0",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"implement",
				null));

		assertThat(rackDevicePorts.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDevicePortId()).isEqualTo(port.getRackDevicePortId());
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackDevicePortTypeId()).isEqualTo(portType.getRackDevicePortTypeId());
			assertThat(row.rackDevicePortName()).isEqualTo("eth0");
		});

		assertThat(rackDevicePorts.listCurrentByRackDevice(device.getRackDeviceId())).hasSize(1);
		assertThat(rackDevicePorts.findCurrent(port.getRackDevicePortId())).get()
				.extracting(RackDevicePortDto::rackDevicePortName)
				.isEqualTo("eth0");
		assertThat(rackDevicePorts.history(port.getRackDevicePortId())).hasSize(1);
	}

	@Test
	void addsRackDevicePortThroughChangeWorkflow() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType("Copper");

		Long portId = applyAdd(
				AssetType.RACK_DEVICE_PORT,
				json(Map.of(
						"rackDevicePortName", "eth0",
						"rackDeviceId", device.rackDeviceId(),
						"rackDevicePortTypeId", portTypeId)))
				.assetIdentityId();

		RackDevicePortDto current = rackDevicePorts.findCurrent(portId).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(device.rackDeviceId());
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(rackDevicePorts.history(portId)).hasSize(1);
		assertThat(rackDevicePorts.listCurrentByRackDevice(device.rackDeviceId()))
				.extracting(RackDevicePortDto::rackDevicePortName)
				.contains("eth0");
	}

	@Test
	void updatesRackDevicePortThroughChangeWorkflow() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType("Copper");
		Long portId = seedRackDevicePort("eth0", device.rackDeviceId(), portTypeId);

		applyUpdateCurrent(
				AssetType.RACK_DEVICE_PORT,
				portId,
				json(Map.of(
						"rackDevicePortName", "eth1",
						"rackDeviceId", device.rackDeviceId(),
						"rackDevicePortTypeId", portTypeId)));

		RackDevicePortDto current = rackDevicePorts.findCurrent(portId).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth1");
		assertThat(current.rackDeviceId()).isEqualTo(device.rackDeviceId());
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevicePorts.history(portId)).hasSize(2);
		assertThat(rackDevicePorts.history(portId).getFirst()).satisfies(prior -> {
			assertThat(prior.rackDevicePortName()).isEqualTo("eth0");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackDevicePortThroughChangeWorkflow() {
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType("Copper");
		Long portId = seedRackDevicePort("eth0", device.rackDeviceId(), portTypeId);

		applyTerminateCurrent(AssetType.RACK_DEVICE_PORT, portId);

		RackDevicePortDto current = rackDevicePorts.findCurrent(portId).orElseThrow();
		assertThat(current.rackDevicePortName()).isEqualTo("eth0");
		assertThat(current.rackDeviceId()).isEqualTo(device.rackDeviceId());
		assertThat(current.rackDevicePortTypeId()).isEqualTo(portTypeId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevicePorts.history(portId)).hasSize(2);
		assertThat(rackDevicePorts.history(portId).getFirst().validTo()).isNotNull();
	}
}
