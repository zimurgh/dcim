package com.dcim.site.datacenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataCenterServiceTests extends ChangeTestSupport {

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
				appliedBy,
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

	@Test
	void addsDataCenterThroughChangeWorkflow() {
		Long dataCenterId = applyAdd(AssetType.DATA_CENTER, json(Map.of("dataCenterName", "LD4")))
				.assetIdentityId();

		DataCenterDto current = dataCenters.findCurrent(dataCenterId).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("LD4");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(dataCenters.history(dataCenterId)).hasSize(1);
		assertThat(dataCenters.listCurrent()).extracting(DataCenterDto::dataCenterName).contains("LD4");
	}

	@Test
	void updatesDataCenterThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter("NY4");

		applyUpdateCurrent(AssetType.DATA_CENTER, dataCenterId, json(Map.of("dataCenterName", "NY5")));

		DataCenterDto current = dataCenters.findCurrent(dataCenterId).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("NY5");
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(dataCenters.history(dataCenterId)).hasSize(2);
		assertThat(dataCenters.history(dataCenterId).getFirst()).satisfies(prior -> {
			assertThat(prior.dataCenterName()).isEqualTo("NY4");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesDataCenterThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter("NY4");

		applyTerminateCurrent(AssetType.DATA_CENTER, dataCenterId);

		DataCenterDto current = dataCenters.findCurrent(dataCenterId).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("NY4");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(dataCenters.history(dataCenterId)).hasSize(2);
		assertThat(dataCenters.history(dataCenterId).getFirst().validTo()).isNotNull();
	}
}
