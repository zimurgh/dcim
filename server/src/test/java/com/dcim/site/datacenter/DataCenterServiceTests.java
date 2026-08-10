package com.dcim.site.datacenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.HistoryLinkRole;

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

	@Autowired
	ChangeService changes;

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

	@Test
	void addsDataCenterThroughChangeWorkflow() {
		ChangeDto draft = changes.createUntracked("{\"dataCenterName\":\"LD4\"}", "tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Add");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.DATA_CENTER);
		});

		DataCenterDto current = dataCenters.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("LD4");
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(dataCenters.history(applied.assetIdentityId())).hasSize(1);
		assertThat(dataCenters.listCurrent()).extracting(DataCenterDto::dataCenterName).contains("LD4");
	}

	@Test
	void updatesDataCenterThroughChangeWorkflow() {
		ChangeDto added = applyAdd("NY4");
		DataCenterDto before = dataCenters.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{\"dataCenterName\":\"NY5\"}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.dataCenterHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		DataCenterDto current = dataCenters.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("NY5");
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(dataCenters.history(added.assetIdentityId())).hasSize(2);
		assertThat(dataCenters.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.dataCenterName()).isEqualTo("NY4");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesDataCenterThroughChangeWorkflow() {
		ChangeDto added = applyAdd("NY4");
		DataCenterDto before = dataCenters.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.dataCenterHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), "tester");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		DataCenterDto current = dataCenters.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.dataCenterName()).isEqualTo("NY4");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(dataCenters.history(added.assetIdentityId())).hasSize(2);
		assertThat(dataCenters.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private ChangeDto applyAdd(String dataCenterName) {
		ChangeDto draft = changes.createUntracked(
				"{\"dataCenterName\":\"" + dataCenterName + "\"}",
				"tester");
		changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), "tester");
	}
}
