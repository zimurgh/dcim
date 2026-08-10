package com.dcim.site.cage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.site.datacenter.DataCenterIdentity;
import com.dcim.site.datacenter.DataCenterIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CageServiceTests extends ChangeTestSupport {

	@Autowired
	CageIdentityRepository cageIdentities;

	@Autowired
	CageHistoryRepository history;

	@Autowired
	DataCenterIdentityRepository dataCenterIdentities;

	@Test
	void listsAndLoadsCurrentCageUnderDataCenter() {
		DataCenterIdentity dataCenter = dataCenterIdentities.save(new DataCenterIdentity());
		CageIdentity cage = cageIdentities.save(new CageIdentity());

		history.save(new CageHistory(
				cage,
				dataCenter,
				"Cage A",
				LocalDate.of(2026, 1, 1),
				null,
				Instant.parse("2026-01-01T12:00:00Z"),
				appliedBy,
				"implement",
				null));

		assertThat(cages.listCurrent()).singleElement().satisfies(row -> {
			assertThat(row.cageId()).isEqualTo(cage.getCageId());
			assertThat(row.dataCenterId()).isEqualTo(dataCenter.getDataCenterId());
			assertThat(row.cageName()).isEqualTo("Cage A");
		});

		assertThat(cages.listCurrentByDataCenter(dataCenter.getDataCenterId())).hasSize(1);
		assertThat(cages.findCurrent(cage.getCageId())).get()
				.extracting(CageDto::cageName)
				.isEqualTo("Cage A");
		assertThat(cages.history(cage.getCageId())).hasSize(1);
	}

	@Test
	void addsCageThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter("NY4");

		Long cageId = applyAdd(
				AssetType.CAGE,
				json(Map.of("cageName", "Cage-A", "dataCenterId", dataCenterId)))
				.assetIdentityId();

		CageDto current = cages.findCurrent(cageId).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-A");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(cages.history(cageId)).hasSize(1);
		assertThat(cages.listCurrentByDataCenter(dataCenterId)).extracting(CageDto::cageName).contains("Cage-A");
	}

	@Test
	void updatesCageThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter("NY4");
		Long cageId = seedCage("Cage-A", dataCenterId);

		applyUpdateCurrent(
				AssetType.CAGE,
				cageId,
				json(Map.of("cageName", "Cage-B", "dataCenterId", dataCenterId)));

		CageDto current = cages.findCurrent(cageId).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-B");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(cages.history(cageId)).hasSize(2);
		assertThat(cages.history(cageId).getFirst()).satisfies(prior -> {
			assertThat(prior.cageName()).isEqualTo("Cage-A");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesCageThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter("NY4");
		Long cageId = seedCage("Cage-A", dataCenterId);

		applyTerminateCurrent(AssetType.CAGE, cageId);

		CageDto current = cages.findCurrent(cageId).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-A");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(cages.history(cageId)).hasSize(2);
		assertThat(cages.history(cageId).getFirst().validTo()).isNotNull();
	}
}
