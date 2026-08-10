package com.dcim.connectivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
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
class DocumentServiceTests {

	@Autowired
	DocumentService documents;

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
	void addsDocumentThroughChangeWorkflow() {
		Long crossConnectId = seedCrossConnect();
		ChangeDto applied = applyAdd(
				AssetType.DOCUMENT,
				"{\"documentName\":\"LOA-1\",\"crossConnectId\":" + crossConnectId + "}");
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.DOCUMENT);
		});

		DocumentDto current = documents.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.documentName()).isEqualTo("LOA-1");
		assertThat(current.crossConnectId()).isEqualTo(crossConnectId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(documents.listCurrentByCrossConnect(crossConnectId)).hasSize(1);
		assertThat(documents.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesDocumentThroughChangeWorkflow() {
		Long crossConnectId = seedCrossConnect();
		Long otherCrossConnectId = seedCrossConnect("XC-2");
		ChangeDto added = applyAdd(
				AssetType.DOCUMENT,
				"{\"documentName\":\"LOA-1\",\"crossConnectId\":" + crossConnectId + "}");
		DocumentDto before = documents.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"documentName\":\"LOA-1B\",\"crossConnectId\":" + otherCrossConnectId + "}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DOCUMENT,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.documentHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		DocumentDto current = documents.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.documentName()).isEqualTo("LOA-1B");
		assertThat(current.crossConnectId()).isEqualTo(otherCrossConnectId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(documents.listCurrentByCrossConnect(crossConnectId)).isEmpty();
		assertThat(documents.listCurrentByCrossConnect(otherCrossConnectId)).hasSize(1);
		assertThat(documents.history(added.assetIdentityId())).hasSize(2);
		assertThat(documents.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesDocumentThroughChangeWorkflow() {
		Long crossConnectId = seedCrossConnect();
		ChangeDto added = applyAdd(
				AssetType.DOCUMENT,
				"{\"documentName\":\"LOA-1\",\"crossConnectId\":" + crossConnectId + "}");
		DocumentDto before = documents.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DOCUMENT,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.documentHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		DocumentDto current = documents.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.documentName()).isEqualTo("LOA-1");
		assertThat(documents.history(added.assetIdentityId())).hasSize(2);
		assertThat(documents.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Long seedCrossConnect() {
		return seedCrossConnect("XC-1");
	}

	private Long seedCrossConnect(String name) {
		Long ownerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"Owner-" + name + "\"}").assetIdentityId();
		Long billingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"Billing-" + name + "\"}").assetIdentityId();
		Long latencyId = applyAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"Low Latency\",\"latencyType\":\"LL\"}")
				.assetIdentityId();
		Long speedId = applyAdd(
				AssetType.SPEED,
				"{\"speedName\":\"1 Gigabit\",\"speedType\":\"1G\"}")
				.assetIdentityId();
		Long crossConnectTypeId = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"Single Mode Fiber\"}")
				.assetIdentityId();
		return applyAdd(
				AssetType.CROSS_CONNECT,
				"{\"crossConnectName\":\"" + name + "\",\"circuitId\":\"CKT-" + name
						+ "\",\"crossConnectTypeId\":" + crossConnectTypeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}")
				.assetIdentityId();
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
