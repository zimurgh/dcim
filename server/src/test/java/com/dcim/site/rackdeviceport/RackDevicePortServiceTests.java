package com.dcim.site.rackdeviceport;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dcim.site.rackdevice.RackDeviceIdentity;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RackDevicePortServiceTests {

	@Autowired
	RackDevicePortService ports;

	@Autowired
	RackDevicePortIdentityRepository portIdentities;

	@Autowired
	RackDevicePortHistoryRepository history;

	@Autowired
	RackDeviceIdentityRepository rackDeviceIdentities;

	@Test
	void listsAndLoadsCurrentPortUnderRackDevice() {
		RackDeviceIdentity device = rackDeviceIdentities.save(new RackDeviceIdentity());
		RackDevicePortIdentity port = portIdentities.save(new RackDevicePortIdentity());

		history.save(new RackDevicePortHistory(
				port,
				device,
				"eth0",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(ports.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackDevicePortId()).isEqualTo(port.getRackDevicePortId());
			assertThat(row.rackDeviceId()).isEqualTo(device.getRackDeviceId());
			assertThat(row.rackDevicePortName()).isEqualTo("eth0");
		});

		assertThat(ports.listCurrentByRackDevice(device.getRackDeviceId())).hasSize(1);
		assertThat(ports.findCurrent(port.getRackDevicePortId())).get()
				.extracting(RackDevicePortDto::rackDevicePortName)
				.isEqualTo("eth0");
		assertThat(ports.history(port.getRackDevicePortId())).hasSize(1);
	}
}
