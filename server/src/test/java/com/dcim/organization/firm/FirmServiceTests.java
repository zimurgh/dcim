package com.dcim.organization.firm;

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
class FirmServiceTests {

	@Autowired
	FirmService firms;

	@Autowired
	FirmIdentityRepository identities;

	@Autowired
	FirmHistoryRepository history;

	@Test
	void listsAndLoadsCurrentFirm() {
		FirmIdentity identity = identities.save(new FirmIdentity());

		history.save(new FirmHistory(
				identity,
				"Acme Trading",
				null,
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				null,
				"implement",
				null));

		assertThat(firms.listCurrent()).singleElement().satisfies(firm -> {
			assertThat(firm.firmId()).isEqualTo(identity.getFirmId());
			assertThat(firm.firmName()).isEqualTo("Acme Trading");
			assertThat(firm.firmHistoryId()).isNotNull();
		});

		assertThat(firms.findCurrent(identity.getFirmId())).get()
				.extracting(FirmDto::firmName)
				.isEqualTo("Acme Trading");
		assertThat(firms.history(identity.getFirmId())).hasSize(1);
	}
}
