package com.dcim.workflow.validation;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;

import org.junit.jupiter.api.Test;

/**
 * Common rules from DESIGN.md that apply to every asset type: payload shape, required fields, enum
 * parsing, optimistic concurrency, and reference liveness. Exercised through a couple of
 * representative asset types (Data Center, Cage, Latency, Cross Connect).
 */
class CommonValidationTests extends ValidationTestSupport {

	// ---- payload shape: only known fields ----

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

	// ---- required fields ----

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

	// ---- enums / kinds ----

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

	// ---- concurrency: stale baseHistoryId ----

	@Test
	void staleBaseHistoryIdRejectsUpdate() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long originalHistoryId = currentHistoryId(AssetType.DATA_CENTER, dataCenterId);

		// Advance the identity so originalHistoryId is no longer current.
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

	// ---- reference liveness: not found ----

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

	// ---- reference liveness: not active ----

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
