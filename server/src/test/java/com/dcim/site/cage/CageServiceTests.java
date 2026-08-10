package com.dcim.site.cage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.datacenter.DataCenterIdentity;
import com.dcim.site.datacenter.DataCenterIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.HistoryLinkRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CageServiceTests {

	@Autowired
	CageService cages;

	@Autowired
	CageIdentityRepository cageIdentities;

	@Autowired
	CageHistoryRepository history;

	@Autowired
	DataCenterIdentityRepository dataCenterIdentities;

	@Autowired
	ChangeService changes;

	@Autowired
	UserIdentityRepository userIdentities;

	@Autowired
	UserHistoryRepository userHistory;

	Long appliedBy;

	@BeforeEach
	void seedUser() {
		appliedBy = TestUsers.seed(userIdentities, userHistory, "tester");
	}

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
		Long dataCenterId = seedDataCenter();

		ChangeDto draft = changes.createUntracked(
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CAGE,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Add");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.CAGE);
		});

		CageDto current = cages.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-A");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.appliedBy()).isEqualTo(appliedBy);
		assertThat(current.validTo()).isNull();
		assertThat(cages.history(applied.assetIdentityId())).hasSize(1);
		assertThat(cages.listCurrentByDataCenter(dataCenterId)).extracting(CageDto::cageName).contains("Cage-A");
	}

	@Test
	void updatesCageThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter();
		ChangeDto added = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}");
		CageDto before = cages.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"cageName\":\"Cage-B\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CAGE,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.cageHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CageDto current = cages.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-B");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();

		assertThat(cages.history(added.assetIdentityId())).hasSize(2);
		assertThat(cages.history(added.assetIdentityId()).getFirst()).satisfies(prior -> {
			assertThat(prior.cageName()).isEqualTo("Cage-A");
			assertThat(prior.validTo()).isNotNull();
		});
	}

	@Test
	void terminatesCageThroughChangeWorkflow() {
		Long dataCenterId = seedDataCenter();
		ChangeDto added = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}");
		CageDto before = cages.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CAGE,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.cageHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CageDto current = cages.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.cageName()).isEqualTo("Cage-A");
		assertThat(current.dataCenterId()).isEqualTo(dataCenterId);
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.validTo()).isNull();

		assertThat(cages.history(added.assetIdentityId())).hasSize(2);
		assertThat(cages.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Long seedDataCenter() {
		return applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}").assetIdentityId();
	}

	private ChangeDto applyAdd(AssetType assetType, String payload) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(
				draft.changeId(),
				assetType,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
