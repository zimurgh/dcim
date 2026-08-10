package com.dcim.connectivity.crossconnect;

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
class CrossConnectServiceTests {

	@Autowired
	CrossConnectService crossConnects;

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
	void addsCrossConnectThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto applied = applyAdd(
				AssetType.CROSS_CONNECT,
				xcPayload("XC-1", seed.crossConnectTypeId(), seed.latencyId(), seed.speedId(), seed.ownerFirmId(),
						seed.billingFirmId(), seed.providerFirmId()));
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.CROSS_CONNECT);
		});

		CrossConnectDto current = crossConnects.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectName()).isEqualTo("XC-1");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1");
		assertThat(current.crossConnectTypeId()).isEqualTo(seed.crossConnectTypeId());
		assertThat(current.latencyId()).isEqualTo(seed.latencyId());
		assertThat(current.speedId()).isEqualTo(seed.speedId());
		assertThat(current.ownerFirmId()).isEqualTo(seed.ownerFirmId());
		assertThat(current.billingFirmId()).isEqualTo(seed.billingFirmId());
		assertThat(current.providerFirmId()).isEqualTo(seed.providerFirmId());
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(crossConnects.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesCrossConnectThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		Long otherBillingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"BillingCo2\"}").assetIdentityId();
		Long otherTypeId = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"Dark Fiber\"}")
				.assetIdentityId();
		Long ullId = applyAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"Ultra Low Latency\",\"latencyType\":\"ULL\"}")
				.assetIdentityId();
		Long speed10gId = applyAdd(
				AssetType.SPEED,
				"{\"speedName\":\"10 Gigabit\",\"speedType\":\"10G\"}")
				.assetIdentityId();

		ChangeDto added = applyAdd(
				AssetType.CROSS_CONNECT,
				xcPayload("XC-1", seed.crossConnectTypeId(), seed.latencyId(), seed.speedId(), seed.ownerFirmId(),
						seed.billingFirmId(), seed.providerFirmId()));
		CrossConnectDto before = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				"{\"crossConnectName\":\"XC-1-REN\",\"circuitId\":\"CKT-XC-1B\",\"crossConnectTypeId\":" + otherTypeId
						+ ",\"latencyId\":" + ullId
						+ ",\"speedId\":" + speed10gId
						+ ",\"ownerFirmId\":" + seed.ownerFirmId()
						+ ",\"billingFirmId\":" + otherBillingFirmId
						+ ",\"providerFirmId\":null}",
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CROSS_CONNECT,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.crossConnectHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CrossConnectDto current = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectName()).isEqualTo("XC-1-REN");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1B");
		assertThat(current.crossConnectTypeId()).isEqualTo(otherTypeId);
		assertThat(current.latencyId()).isEqualTo(ullId);
		assertThat(current.speedId()).isEqualTo(speed10gId);
		assertThat(current.billingFirmId()).isEqualTo(otherBillingFirmId);
		assertThat(current.providerFirmId()).isNull();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(crossConnects.history(added.assetIdentityId())).hasSize(2);
		assertThat(crossConnects.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesCrossConnectThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto added = applyAdd(
				AssetType.CROSS_CONNECT,
				xcPayload("XC-1", seed.crossConnectTypeId(), seed.latencyId(), seed.speedId(), seed.ownerFirmId(),
						seed.billingFirmId(), seed.providerFirmId()));
		CrossConnectDto before = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.CROSS_CONNECT,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.crossConnectHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		CrossConnectDto current = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.crossConnectName()).isEqualTo("XC-1");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1");
		assertThat(current.crossConnectTypeId()).isEqualTo(seed.crossConnectTypeId());
		assertThat(crossConnects.history(added.assetIdentityId())).hasSize(2);
		assertThat(crossConnects.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Seed seedDependencies() {
		Long ownerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"OwnerCo\"}").assetIdentityId();
		Long billingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"BillingCo\"}").assetIdentityId();
		Long providerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"ProviderCo\"}").assetIdentityId();
		Long crossConnectTypeId = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"Single Mode Fiber\"}")
				.assetIdentityId();
		Long latencyId = applyAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"Low Latency\",\"latencyType\":\"LL\"}")
				.assetIdentityId();
		Long speedId = applyAdd(
				AssetType.SPEED,
				"{\"speedName\":\"1 Gigabit\",\"speedType\":\"1G\"}")
				.assetIdentityId();
		return new Seed(ownerFirmId, billingFirmId, providerFirmId, crossConnectTypeId, latencyId, speedId);
	}

	private static String xcPayload(
			String name,
			Long crossConnectTypeId,
			Long latencyId,
			Long speedId,
			Long ownerFirmId,
			Long billingFirmId,
			Long providerFirmId) {
		return "{\"crossConnectName\":\"" + name + "\",\"circuitId\":\"CKT-" + name
				+ "\",\"crossConnectTypeId\":" + crossConnectTypeId
				+ ",\"latencyId\":" + latencyId
				+ ",\"speedId\":" + speedId
				+ ",\"ownerFirmId\":" + ownerFirmId
				+ ",\"billingFirmId\":" + billingFirmId
				+ ",\"providerFirmId\":" + providerFirmId + "}";
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

	private record Seed(
			Long ownerFirmId,
			Long billingFirmId,
			Long providerFirmId,
			Long crossConnectTypeId,
			Long latencyId,
			Long speedId) {
	}
}
