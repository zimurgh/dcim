package com.dcim.site.rack;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.dcim.site.cage.CageIdentity;
import com.dcim.site.cage.CageIdentityRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RackServiceTests {

	@Autowired
	RackService racks;

	@Autowired
	RackIdentityRepository rackIdentities;

	@Autowired
	RackHistoryRepository history;

	@Autowired
	CageIdentityRepository cageIdentities;

	@Test
	void listsAndLoadsCurrentRackUnderCage() {
		CageIdentity cage = cageIdentities.save(new CageIdentity());
		RackIdentity rack = rackIdentities.save(new RackIdentity());

		history.save(new RackHistory(
				rack,
				cage,
				"R01",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				"tester",
				"implement",
				null));

		assertThat(racks.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.rackId()).isEqualTo(rack.getRackId());
			assertThat(row.cageId()).isEqualTo(cage.getCageId());
			assertThat(row.rackName()).isEqualTo("R01");
		});

		assertThat(racks.listCurrentByCage(cage.getCageId())).hasSize(1);
		assertThat(racks.findCurrent(rack.getRackId())).get()
				.extracting(RackDto::rackName)
				.isEqualTo("R01");
		assertThat(racks.history(rack.getRackId())).hasSize(1);
	}
}
