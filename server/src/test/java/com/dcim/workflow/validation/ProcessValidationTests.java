package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationIssue;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeSpecDto;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.ChangeValidationResult;

import org.junit.jupiter.api.Test;

/**
 * Process-level rules from DESIGN.md "Change validation": save independent of validation, apply as a
 * hard gate, batch-aware validation across a Change Spec, and Change Spec validate aggregation.
 */
class ProcessValidationTests extends ValidationTestSupport {

	@Test
	void saveIndependentOfValidation_untrackedAndStagedAcceptInvalidPayloadWithoutValidating() {
		ChangeDto draft = changes.createUntracked("{\"bogus\":true}", "tester");
		assertThat(draft.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto amended = changes.amendPayload(draft.changeId(), "{\"stillBogus\":1}", "tester");
		assertThat(amended.stage()).isEqualTo(ChangeStage.UNTRACKED);

		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(), AssetType.DATA_CENTER, ChangeAction.ADD, null, null,
				"{\"unknownField\":\"x\"}", "tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);

		assertInvalid(draft.changeId(), ValidationCodes.UNKNOWN_FIELD);
	}

	@Test
	void applyIsHardGate_blocksAndLeavesLedgerUnmutatedWhenIssuesRemain() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"unknownField\":\"x\"}");

		assertApplyBlocked(staged.changeId(), ValidationCodes.UNKNOWN_FIELD);

		assertThat(dataCenters.listCurrent()).isEmpty();
	}

	@Test
	void applyIsHardGate_succeedsOnceIssuesAreResolved() {
		ChangeDto staged = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void batchAwareTerminate_parentAloneBlockedByLiveChild() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		seedCage(unique("Cage"), dataCenterId);

		ChangeDto parentAlone = stageTerminateCurrent(AssetType.DATA_CENTER, dataCenterId);
		assertInvalid(parentAlone.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(parentAlone.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void batchAwareTerminate_parentAndChildTogetherOnChangeSpecPass() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		Long ownerFirmId = seedFirm(unique("Owner"));

		ChangeDto terminateCage = stageTerminateCurrent(AssetType.CAGE, cageId);
		ChangeDto terminateDataCenter = stageTerminateCurrent(AssetType.DATA_CENTER, dataCenterId);

		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), terminateCage.changeId());
		addToSpec(spec.changeSpecId(), terminateDataCenter.changeId());
		submitPendingBillingWithChrec(spec.changeSpecId());

		assertSpecValid(spec.changeSpecId());
		assertSpecApplySucceeds(spec.changeSpecId());

		assertThat(dataCenters.findCurrent(dataCenterId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(cages.findCurrent(cageId).orElseThrow().status()).isEqualTo("Terminated");
	}

	@Test
	void changeSpecValidate_aggregatesIssuesFromEveryMember() {
		Long ownerFirmId = seedFirm(unique("Owner"));

		ChangeDto badDataCenter = stageAdd(AssetType.DATA_CENTER, "{}");
		ChangeDto badCage = stageAdd(AssetType.CAGE, "{\"cageName\":\"Cage-X\",\"dataCenterId\":999999}");

		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), badDataCenter.changeId());
		addToSpec(spec.changeSpecId(), badCage.changeId());

		ChangeValidationResult result = changeSpecs.validate(spec.changeSpecId());
		assertThat(result.ok()).isFalse();
		assertThat(result.issues()).extracting(ValidationIssue::code)
				.contains(ValidationCodes.MISSING_FIELD, ValidationCodes.REFERENCE_NOT_FOUND);
		// Aggregated messages are change-id-prefixed so issues stay traceable to their source change.
		assertThat(result.issues()).extracting(ValidationIssue::message)
				.anySatisfy(message -> assertThat(message).contains("Change " + badDataCenter.changeId()))
				.anySatisfy(message -> assertThat(message).contains("Change " + badCage.changeId()));
	}
}
