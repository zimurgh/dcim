package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcim.asset.ValidationCodes;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;
import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.site.cage.CageDto;
import com.dcim.site.cage.CageService;
import com.dcim.site.datacenter.DataCenterDto;
import com.dcim.site.datacenter.DataCenterService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChangeValidationTests {

	@Autowired
	ChangeService changes;

	@Autowired
	DataCenterService dataCenters;

	@Autowired
	CageService cages;

	@Autowired
	CrossConnectService crossConnects;

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
	void amendAllowsInvalidPayloadWithoutValidation() {
		ChangeDto draft = changes.createUntracked("{\"bogus\":true}", "tester");
		ChangeDto amended = changes.amendPayload(draft.changeId(), "{\"stillBogus\":1}", "tester");
		assertThat(amended.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				"{\"unknownField\":\"x\"}",
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);

		ChangeValidationResult result = changes.validateStaged(draft.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> {
			assertThat(issue.code()).isEqualTo(ValidationCodes.UNKNOWN_FIELD);
			assertThat(issue.field()).isEqualTo("unknownField");
		});
	}

	@Test
	void applyBlockedWhenValidationFails() {
		ChangeDto draft = changes.createUntracked("{\"unknownField\":\"x\"}", "tester");
		changes.promoteToStaged(
				draft.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");

		assertThatThrownBy(() -> changes.applyStaged(draft.changeId(), appliedBy))
				.isInstanceOf(ValidationFailedException.class)
				.satisfies(ex -> {
					ValidationFailedException failed = (ValidationFailedException) ex;
					assertThat(failed.getIssues()).isNotEmpty();
					assertThat(failed.getIssues().getFirst().code()).isEqualTo(ValidationCodes.UNKNOWN_FIELD);
				});

		assertThat(dataCenters.listCurrent()).isEmpty();
	}

	@Test
	void nameClashBlocksSecondDataCenterAdd() {
		applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");

		ChangeDto clash = changes.createUntracked("{\"dataCenterName\":\"NY4\"}", "tester");
		changes.promoteToStaged(
				clash.changeId(), AssetType.DATA_CENTER, ChangeAction.ADD, null, null, null, "tester");

		ChangeValidationResult result = changes.validateStaged(clash.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> assertThat(issue.code()).isEqualTo(ValidationCodes.NAME_CLASH));

		assertThatThrownBy(() -> changes.applyStaged(clash.changeId(), appliedBy))
				.isInstanceOf(ValidationFailedException.class);
	}

	@Test
	void terminateDataCenterBlockedByLiveCage() {
		Long dataCenterId = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}").assetIdentityId();
		applyAdd(AssetType.CAGE, "{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}");
		DataCenterDto current = dataCenters.findCurrent(dataCenterId).orElseThrow();

		ChangeDto terminate = changes.createUntracked("{}", "tester");
		changes.promoteToStaged(
				terminate.changeId(),
				AssetType.DATA_CENTER,
				ChangeAction.TERMINATE,
				dataCenterId,
				current.dataCenterHistoryId(),
				null,
				"tester");

		ChangeValidationResult result = changes.validateStaged(terminate.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> {
			assertThat(issue.code()).isEqualTo(ValidationCodes.ACTIVE_CHILDREN);
			assertThat(issue.relatedIdentityIds()).isNotEmpty();
		});
	}

	@Test
	void circuitIdClashBlocksSecondCrossConnect() {
		Long ownerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"OwnerCo\"}").assetIdentityId();
		Long billingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"BillingCo\"}").assetIdentityId();
		Long typeId = applyAdd(
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

		String payload = "{\"crossConnectName\":\"XC-1\",\"circuitId\":\"CKT-DUP\",\"crossConnectTypeId\":" + typeId
				+ ",\"latencyId\":" + latencyId
				+ ",\"speedId\":" + speedId
				+ ",\"ownerFirmId\":" + ownerFirmId
				+ ",\"billingFirmId\":" + billingFirmId + "}";
		applyAdd(AssetType.CROSS_CONNECT, payload);

		ChangeDto clash = changes.createUntracked(
				"{\"crossConnectName\":\"XC-2\",\"circuitId\":\"CKT-DUP\",\"crossConnectTypeId\":" + typeId
						+ ",\"latencyId\":" + latencyId
						+ ",\"speedId\":" + speedId
						+ ",\"ownerFirmId\":" + ownerFirmId
						+ ",\"billingFirmId\":" + billingFirmId + "}",
				"tester");
		changes.promoteToStaged(
				clash.changeId(), AssetType.CROSS_CONNECT, ChangeAction.ADD, null, null, null, "tester");

		ChangeValidationResult result = changes.validateStaged(clash.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> assertThat(issue.code()).isEqualTo(ValidationCodes.VALUE_CLASH));
		assertThat(crossConnects.listCurrent()).hasSize(1);
	}

	@Test
	void missingRequiredFieldReportedOnValidate() {
		ChangeDto draft = changes.createUntracked("{}", "tester");
		changes.promoteToStaged(
				draft.changeId(), AssetType.DATA_CENTER, ChangeAction.ADD, null, null, null, "tester");

		ChangeValidationResult result = changes.validateStaged(draft.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> {
			assertThat(issue.code()).isEqualTo(ValidationCodes.MISSING_FIELD);
			assertThat(issue.field()).isEqualTo("dataCenterName");
		});
	}

	@Test
	void scopedCageNameClashWithinDataCenter() {
		Long dataCenterId = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}").assetIdentityId();
		applyAdd(AssetType.CAGE, "{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}");

		ChangeDto clash = changes.createUntracked(
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenterId + "}",
				"tester");
		changes.promoteToStaged(clash.changeId(), AssetType.CAGE, ChangeAction.ADD, null, null, null, "tester");

		ChangeValidationResult result = changes.validateStaged(clash.changeId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).anySatisfy(issue -> assertThat(issue.code()).isEqualTo(ValidationCodes.NAME_CLASH));

		CageDto remaining = cages.listCurrent().getFirst();
		assertThat(remaining.cageName()).isEqualTo("Cage-A");
	}

	private ChangeDto applyAdd(AssetType assetType, String payload) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(draft.changeId(), assetType, ChangeAction.ADD, null, null, null, "tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
