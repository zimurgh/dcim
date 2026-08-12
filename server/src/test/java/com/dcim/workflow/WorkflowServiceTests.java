package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.cage.CageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowServiceTests {

	@Autowired
	ChangeService changes;

	@Autowired
	ChangeSpecService specs;

	@Autowired
	CageService cages;

	@Autowired
	ChangeUntrackedRepository untracked;

	@Autowired
	ChangeStagedRepository staged;

	@Autowired
	ChangeCommittedRepository committed;

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
	void untrackedToStagedToCommitted() {
		Long dataCenterId = changes.applyStaged(
				changes.promoteToStaged(
						changes.createUntracked("{\"dataCenterName\":\"NY4\"}", "tester").changeId(),
						"DATA_CENTER",
						ChangeAction.ADD,
						null,
						null,
						null,
						"tester").changeId(),
				appliedBy).assetIdentityId();

		ChangeDto draft = changes.createUntracked(
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);
		assertThat(draft.statusLabel()).isEqualTo("Draft");
		assertThat(untracked.existsById(draft.changeId())).isTrue();

		ChangeDto amended = changes.amendPayload(
				draft.changeId(),
				"{\"cageName\":\"Cage-B\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		assertThat(amended.body()).contains("Cage-B");
		assertThat(amended.payloadId()).isNotEqualTo(draft.payloadId());

		ChangeDto stagedChange = changes.promoteToStaged(
				draft.changeId(),
				"CAGE",
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		assertThat(stagedChange.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(stagedChange.statusLabel()).isEqualTo("Pending Add");
		assertThat(untracked.existsById(draft.changeId())).isFalse();
		assertThat(staged.existsById(draft.changeId())).isTrue();

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.appliedBy()).isEqualTo(appliedBy);
		assertThat(applied.historyLinks()).singleElement()
				.extracting(ChangeDto.HistoryLinkDto::role)
				.isEqualTo(HistoryLinkRole.CREATED);
		assertThat(staged.existsById(draft.changeId())).isFalse();
		assertThat(committed.existsById(draft.changeId())).isTrue();
		assertThat(cages.findCurrent(applied.assetIdentityId())).get()
				.extracting(com.dcim.site.cage.CageDto::cageName)
				.isEqualTo("Cage-B");
	}

	@Test
	void changeSpecRequiresChrecBeforePendingBillingAndApply() {
		Long firmId = changes.applyStaged(
				changes.promoteToStaged(
						changes.createUntracked("{\"firmName\":\"Owner\"}", "tester").changeId(),
						"FIRM",
						ChangeAction.ADD,
						null,
						null,
						null,
						"tester").changeId(),
				appliedBy).assetIdentityId();
		Long dataCenterId = changes.applyStaged(
				changes.promoteToStaged(
						changes.createUntracked("{\"dataCenterName\":\"NY4\"}", "tester").changeId(),
						"DATA_CENTER",
						ChangeAction.ADD,
						null,
						null,
						null,
						"tester").changeId(),
				appliedBy).assetIdentityId();

		ChangeSpecDto spec = specs.create(firmId, "Cage work", "tester");
		assertThat(spec.status()).isEqualTo(ChangeSpecStatus.DRAFT);

		ChangeDto change = changes.createUntracked(
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		changes.promoteToStaged(change.changeId(), "CAGE", ChangeAction.ADD, null, null, null, "tester");
		specs.addChange(spec.changeSpecId(), change.changeId());

		assertThatThrownBy(() -> specs.submitPendingBilling(spec.changeSpecId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("CHREC");

		specs.linkChrec(spec.changeSpecId(), "CHREC-1", "Install cage", "https://jira.example/CHREC-1");
		ChangeSpecDto pending = specs.submitPendingBilling(spec.changeSpecId());
		assertThat(pending.status()).isEqualTo(ChangeSpecStatus.PENDING_BILLING);
		assertThat(pending.chrecs()).singleElement().extracting(ChangeSpecDto.ChrecDto::jiraKey)
				.isEqualTo("CHREC-1");

		ChangeSpecDto applied = specs.apply(spec.changeSpecId(), appliedBy);
		assertThat(applied.status()).isEqualTo(ChangeSpecStatus.APPLIED);
		assertThat(committed.existsById(change.changeId())).isTrue();
		assertThat(staged.existsById(change.changeId())).isFalse();
		assertThat(cages.listCurrent()).extracting(com.dcim.site.cage.CageDto::cageName)
				.contains("Cage-A");
	}
}
