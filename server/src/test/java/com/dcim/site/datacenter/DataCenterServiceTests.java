package com.dcim.site.datacenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataCenterServiceTests {

	@Autowired
	DataCenterService dataCenters;

	@Autowired
	DataCenterIdentityRepository identities;

	@Autowired
	DataCenterHistoryRepository history;

	@Test
	void listsAndLoadsCurrentDataCenter() {
		DataCenterIdentity identity = identities.save(new DataCenterIdentity());

		history.save(new DataCenterHistory(
				identity,
				"NY4",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(dataCenters.listCurrent()).singleElement().satisfies(dc -> {
			assertThat(dc.dataCenterId()).isEqualTo(identity.getDataCenterId());
			assertThat(dc.dataCenterName()).isEqualTo("NY4");
			assertThat(dc.dataCenterHistoryId()).isNotNull();
		});

		assertThat(dataCenters.findCurrent(identity.getDataCenterId())).get()
				.extracting(DataCenterDto::dataCenterName)
				.isEqualTo("NY4");
		assertThat(dataCenters.history(identity.getDataCenterId())).hasSize(1);
	}
}
