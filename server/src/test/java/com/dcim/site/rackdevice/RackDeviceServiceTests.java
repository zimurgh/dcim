package com.dcim.site.rackdevice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rack.RackIdentityRepository;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentity;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentityRepository;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RackDeviceServiceTests extends ChangeTestSupport {

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Autowired
	RackDeviceHistoryRepository history;

	@Autowired
	RackIdentityRepository rackIdentities;

	@Autowired
	RackDeviceTypeIdentityRepository deviceTypeIdentities;

	@Test
	void listsAndLoadsCurrentRackDeviceUnderRack() {
		RackIdentity rack = rackIdentities.save(new RackIdentity());
		RackDeviceTypeIdentity deviceType = deviceTypeIdentities.save(new RackDeviceTypeIdentity());
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());

		history.save(new RackDeviceHistory(
				device,
				rack,
				deviceType,
				"sw-01",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"implement",
				null));

		assertThat(rackDevices.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackId()).isEqualTo(rack.getRackId());
			assertThat(row.rackDeviceTypeId()).isEqualTo(deviceType.getRackDeviceTypeId());
			assertThat(row.rackDeviceName()).isEqualTo("sw-01");
		});

		assertThat(rackDevices.listCurrentByRack(rack.getRackId())).hasSize(1);
		assertThat(rackDevices.findCurrent(device.getRackDeviceId())).get()
				.extracting(RackDeviceDto::rackDeviceName)
				.isEqualTo("sw-01");
		assertThat(rackDevices.history(device.getRackDeviceId())).hasSize(1);
	}

	@Test
	void addsRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack("R01", seedCage("Cage-A", seedDataCenter("NY4")));
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");

		Long deviceId = applyAdd(
				"RACK_DEVICE",
				json(Map.of(
						"rackDeviceName", "sw-01",
						"rackId", rackId,
						"rackDeviceTypeId", deviceTypeId)))
				.assetIdentityId();

		RackDeviceDto current = rackDevices.findCurrent(deviceId).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-01");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(rackDevices.history(deviceId)).hasSize(1);
		assertThat(rackDevices.listCurrentByRack(rackId)).extracting(RackDeviceDto::rackDeviceName).contains("sw-01");
	}

	@Test
	void updatesRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack("R01", seedCage("Cage-A", seedDataCenter("NY4")));
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");
		Long deviceId = seedRackDevice("sw-01", rackId, deviceTypeId);

		applyUpdateCurrent(
				"RACK_DEVICE",
				deviceId,
				json(Map.of(
						"rackDeviceName", "sw-02",
						"rackId", rackId,
						"rackDeviceTypeId", deviceTypeId)));

		RackDeviceDto current = rackDevices.findCurrent(deviceId).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-02");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevices.history(deviceId)).hasSize(2);
		assertThat(rackDevices.history(deviceId).getFirst()).satisfies(prior -> {
			assertThat(prior.rackDeviceName()).isEqualTo("sw-01");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackDeviceThroughChangeWorkflow() {
		Long rackId = seedRack("R01", seedCage("Cage-A", seedDataCenter("NY4")));
		Long deviceTypeId = seedRackDeviceType("Extranet Switch", "EXTRANET_SWITCH");
		Long deviceId = seedRackDevice("sw-01", rackId, deviceTypeId);

		applyTerminateCurrent("RACK_DEVICE", deviceId);

		RackDeviceDto current = rackDevices.findCurrent(deviceId).orElseThrow();
		assertThat(current.rackDeviceName()).isEqualTo("sw-01");
		assertThat(current.rackId()).isEqualTo(rackId);
		assertThat(current.rackDeviceTypeId()).isEqualTo(deviceTypeId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(rackDevices.history(deviceId)).hasSize(2);
		assertThat(rackDevices.history(deviceId).getFirst().validTo()).isNotNull();
	}
}
