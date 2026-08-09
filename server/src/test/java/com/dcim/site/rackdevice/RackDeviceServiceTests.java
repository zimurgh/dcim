package com.dcim.site.rackdevice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rack.RackIdentityRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RackDeviceServiceTests {

	@Autowired
	RackDeviceService rackDevices;

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Autowired
	RackDeviceHistoryRepository history;

	@Autowired
	RackIdentityRepository rackIdentities;

	@Test
	void listsAndLoadsCurrentRackDeviceUnderRack() {
		RackIdentity rack = rackIdentities.save(new RackIdentity());
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());

		history.save(new RackDeviceHistory(
				device,
				rack,
				"sw-01",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(rackDevices.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackId()).isEqualTo(rack.getRackId());
			assertThat(row.rackDeviceName()).isEqualTo("sw-01");
		});

		assertThat(rackDevices.listCurrentByRack(rack.getRackId())).hasSize(1);
		assertThat(rackDevices.findCurrent(device.getRackDeviceId())).get()
				.extracting(RackDeviceDto::rackDeviceName)
				.isEqualTo("sw-01");
		assertThat(rackDevices.history(device.getRackDeviceId())).hasSize(1);
	}
}
