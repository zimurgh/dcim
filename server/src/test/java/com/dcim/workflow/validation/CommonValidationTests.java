package com.dcim.workflow.validation;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;

import org.junit.jupiter.api.Test;

class CommonValidationTests extends ValidationTestSupport {

	@Test
	void unknownFieldRejectsAdd() {
		ChangeDto staged = stageAdd(
				AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\",\"bogusField\":1}");
		assertInvalid(staged.changeId(), ValidationCodes.UNKNOWN_FIELD);
		assertApplyBlocked(staged.changeId(), ValidationCodes.UNKNOWN_FIELD);
	}

	@Test
	void onlyKnownFieldsAllowsAdd() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void missingRequiredFieldRejectsAdd() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{}");
		assertInvalid(staged.changeId(), ValidationCodes.MISSING_FIELD);
		assertApplyBlocked(staged.changeId(), ValidationCodes.MISSING_FIELD);
	}

	@Test
	void presentRequiredFieldAllowsAdd() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void invalidEnumValueRejectsAdd() {
		ChangeDto staged = stageAdd(
				AssetType.LATENCY, "{\"latencyName\":\"" + unique("Latency") + "\",\"latencyType\":\"SLOW\"}");
		assertInvalid(staged.changeId(), ValidationCodes.INVALID_VALUE);
		assertApplyBlocked(staged.changeId(), ValidationCodes.INVALID_VALUE);
	}

	@Test
	void validEnumValueAllowsAdd() {
		ChangeDto staged = stageAdd(
				AssetType.LATENCY, "{\"latencyName\":\"" + unique("Latency") + "\",\"latencyType\":\"LL\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void staleBaseHistoryIdRejectsUpdate() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long originalHistoryId = currentHistoryId(AssetType.DATA_CENTER, dataCenterId);

		ChangeDto rename = stageUpdate(
				AssetType.DATA_CENTER, dataCenterId, originalHistoryId,
				"{\"dataCenterName\":\"" + unique("NY-Renamed") + "\"}");
		assertApplySucceeds(rename.changeId());

		ChangeDto staleUpdate = stageUpdate(
				AssetType.DATA_CENTER, dataCenterId, originalHistoryId,
				"{\"dataCenterName\":\"" + unique("NY-StillStale") + "\"}");
		assertInvalid(staleUpdate.changeId(), ValidationCodes.STALE_BASE);
		assertApplyBlocked(staleUpdate.changeId(), ValidationCodes.STALE_BASE);
	}

	@Test
	void currentBaseHistoryIdAllowsUpdate() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long currentHistoryId = currentHistoryId(AssetType.DATA_CENTER, dataCenterId);

		ChangeDto update = stageUpdate(
				AssetType.DATA_CENTER, dataCenterId, currentHistoryId,
				"{\"dataCenterName\":\"" + unique("NY-Renamed") + "\"}");
		assertValid(update.changeId());
		assertApplySucceeds(update.changeId());
	}

	@Test
	void staleBaseHistoryIdRejectsTerminate() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long originalHistoryId = currentHistoryId(AssetType.DATA_CENTER, dataCenterId);

		ChangeDto rename = stageUpdate(
				AssetType.DATA_CENTER, dataCenterId, originalHistoryId,
				"{\"dataCenterName\":\"" + unique("NY-Renamed") + "\"}");
		assertApplySucceeds(rename.changeId());

		ChangeDto staleTerminate = stageTerminate(AssetType.DATA_CENTER, dataCenterId, originalHistoryId);
		assertInvalid(staleTerminate.changeId(), ValidationCodes.STALE_BASE);
		assertApplyBlocked(staleTerminate.changeId(), ValidationCodes.STALE_BASE);
	}

	@Test
	void historyNotFoundRejectsUpdate() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		ChangeDto staged = stageUpdate(
				AssetType.DATA_CENTER, dataCenterId, 999_999_999L,
				"{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.HISTORY_NOT_FOUND);
		assertApplyBlocked(staged.changeId(), ValidationCodes.HISTORY_NOT_FOUND);
	}

	@Test
	void identityMismatchRejectsUpdate() {
		Long firmA = seedFirm(unique("A"));
		Long firmB = seedFirm(unique("B"));
		Long historyA = currentHistoryId(AssetType.FIRM, firmA);

		ChangeDto staged = stageUpdate(
				AssetType.FIRM, firmB, historyA,
				"{\"firmName\":\"" + unique("Mismatch") + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.IDENTITY_MISMATCH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.IDENTITY_MISMATCH);
	}

	@Test
	void invalidPayloadJsonRejectsValidateAndApply() {
		ChangeDto draft = changes.createUntracked("{not-json", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(), AssetType.DATA_CENTER, com.dcim.workflow.ChangeAction.ADD,
				null, null, null, "tester");
		assertInvalid(staged.changeId(), ValidationCodes.INVALID_PAYLOAD);
		assertApplyBlocked(staged.changeId(), ValidationCodes.INVALID_PAYLOAD);
	}

	@Test
	void referenceNotFoundRejectsAdd() {
		ChangeDto staged = stageAdd(
				AssetType.CAGE, "{\"cageName\":\"" + unique("Cage") + "\",\"dataCenterId\":999999}");
		assertInvalid(staged.changeId(), ValidationCodes.REFERENCE_NOT_FOUND);
		assertApplyBlocked(staged.changeId(), ValidationCodes.REFERENCE_NOT_FOUND);
	}

	@Test
	void referenceFoundAllowsAdd() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		ChangeDto staged = stageAdd(
				AssetType.CAGE, "{\"cageName\":\"" + unique("Cage") + "\",\"dataCenterId\":" + dataCenterId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void referenceNotActiveRejectsAdd() {
		Long firmId = seedFirm(unique("TerminatedOwner"));
		assertApplySucceeds(stageTerminateCurrent(AssetType.FIRM, firmId).changeId());

		XcDeps deps = seedXcDeps();
		String payload = "{\"crossConnectName\":\"XC-Bad\",\"circuitId\":\"" + unique("CKT") + "\""
				+ ",\"crossConnectTypeId\":" + deps.crossConnectTypeId()
				+ ",\"latencyId\":" + deps.latencyId()
				+ ",\"speedId\":" + deps.speedId()
				+ ",\"ownerFirmId\":" + firmId
				+ ",\"billingFirmId\":" + deps.billingFirmId() + "}";

		ChangeDto staged = stageAdd(AssetType.CROSS_CONNECT, payload);
		assertInvalid(staged.changeId(), ValidationCodes.REFERENCE_NOT_ACTIVE);
		assertApplyBlocked(staged.changeId(), ValidationCodes.REFERENCE_NOT_ACTIVE);
	}

	@Test
	void referenceActiveAllowsAdd() {
		XcDeps deps = seedXcDeps();
		ChangeDto staged = stageAdd(AssetType.CROSS_CONNECT, xcPayload(unique("CKT"), deps));
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}
}
