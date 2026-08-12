package com.dcim.site.rack;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.site.cage.CageIdentity;
import com.dcim.site.cage.CageIdentityRepository;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RackServiceTests extends ChangeTestSupport {

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
				appliedBy,
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

	@Test
	void addsRackThroughChangeWorkflow() {
		Long cageId = seedCage("Cage-A", seedDataCenter("NY4"));

		Long rackId = applyAdd("RACK", json(Map.of("rackName", "R01", "cageId", cageId)))
				.assetIdentityId();

		RackDto current = racks.findCurrent(rackId).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R01");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(racks.history(rackId)).hasSize(1);
		assertThat(racks.listCurrentByCage(cageId)).extracting(RackDto::rackName).contains("R01");
	}

	@Test
	void updatesRackThroughChangeWorkflow() {
		Long cageId = seedCage("Cage-A", seedDataCenter("NY4"));
		Long rackId = seedRack("R01", cageId);

		applyUpdateCurrent("RACK", rackId, json(Map.of("rackName", "R02", "cageId", cageId)));

		RackDto current = racks.findCurrent(rackId).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R02");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(racks.history(rackId)).hasSize(2);
		assertThat(racks.history(rackId).getFirst()).satisfies(prior -> {
			assertThat(prior.rackName()).isEqualTo("R01");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesRackThroughChangeWorkflow() {
		Long cageId = seedCage("Cage-A", seedDataCenter("NY4"));
		Long rackId = seedRack("R01", cageId);

		applyTerminateCurrent("RACK", rackId);

		RackDto current = racks.findCurrent(rackId).orElseThrow();
		assertThat(current.rackName()).isEqualTo("R01");
		assertThat(current.cageId()).isEqualTo(cageId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(racks.history(rackId)).hasSize(2);
		assertThat(racks.history(rackId).getFirst().validTo()).isNotNull();
	}
}
