package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dcim.workflow.validation.ValidationTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChangeProcessTests extends ValidationTestSupport {

	@Autowired
	ChangeSpecService changeSpecs;

	@Autowired
	ChangeUntrackedRepository untracked;

	@Autowired
	ChangeStagedRepository staged;

	@Autowired
	ChangeCommittedRepository committed;

	@Test
	void cancelOpenRemovesUntrackedAndStagedButNotCommitted() {
		ChangeDto draft = changes.createUntracked("{\"dataCenterName\":\"" + unique("NY") + "\"}", "tester");
		changes.cancelOpen(draft.changeId());
		assertThat(untracked.existsById(draft.changeId())).isFalse();
		assertThat(changes.find(draft.changeId())).isEmpty();

		ChangeDto stagedChange = stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		changes.cancelOpen(stagedChange.changeId());
		assertThat(staged.existsById(stagedChange.changeId())).isFalse();

		ChangeDto applied = applyAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		assertThatThrownBy(() -> changes.cancelOpen(applied.changeId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("Committed");
		assertThat(committed.existsById(applied.changeId())).isTrue();
	}

	@Test
	void cancelOpenBlockedWhileChangeIsOnChangeSpec() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeDto stagedChange = stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), stagedChange.changeId());

		assertThatThrownBy(() -> changes.cancelOpen(stagedChange.changeId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("Change Spec");
		assertThat(staged.existsById(stagedChange.changeId())).isTrue();
	}

	@Test
	void removeChangeFromDraftSpecThenCancelOpenSucceeds() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeDto stagedChange = stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), stagedChange.changeId());

		ChangeSpecDto afterRemove = changeSpecs.removeChange(spec.changeSpecId(), stagedChange.changeId());
		assertThat(afterRemove.changeIds()).doesNotContain(stagedChange.changeId());

		changes.cancelOpen(stagedChange.changeId());
		assertThat(staged.existsById(stagedChange.changeId())).isFalse();
	}

	@Test
	void cancelSpecMakesItImmutable() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeSpecDto spec = createSpec(ownerFirmId);
		ChangeSpecDto cancelled = changeSpecs.cancel(spec.changeSpecId());
		assertThat(cancelled.status()).isEqualTo(ChangeSpecStatus.CANCELLED);

		assertThatThrownBy(() -> changeSpecs.addChange(
						cancelled.changeSpecId(),
						stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}").changeId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("not mutable");
		assertThatThrownBy(() -> changeSpecs.cancel(cancelled.changeSpecId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("not mutable");
	}

	@Test
	void appliedSpecIsImmutable() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeDto stagedChange = stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), stagedChange.changeId());
		submitPendingBillingWithChrec(spec.changeSpecId());
		assertSpecApplySucceeds(spec.changeSpecId());

		assertThatThrownBy(() -> changeSpecs.removeChange(spec.changeSpecId(), stagedChange.changeId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("not mutable");
		assertThatThrownBy(() -> changeSpecs.cancel(spec.changeSpecId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("not mutable");
	}

	@Test
	void unlinkLastChrecBlockedAfterDraft() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeSpecDto spec = createSpec(ownerFirmId);
		ChangeSpecDto linked = changeSpecs.linkChrec(
				spec.changeSpecId(), unique("CHREC"), "Title", "https://jira.example/1");
		Long chrecId = linked.chrecs().getFirst().chrecId();
		changeSpecs.submitPendingBilling(spec.changeSpecId());

		assertThatThrownBy(() -> changeSpecs.unlinkChrec(spec.changeSpecId(), chrecId))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("CHREC");
	}

	@Test
	void unlinkChrecAllowedInDraftEvenWhenLast() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeSpecDto spec = createSpec(ownerFirmId);
		ChangeSpecDto linked = changeSpecs.linkChrec(
				spec.changeSpecId(), unique("CHREC"), "Title", "https://jira.example/1");
		Long chrecId = linked.chrecs().getFirst().chrecId();

		ChangeSpecDto unlinked = changeSpecs.unlinkChrec(spec.changeSpecId(), chrecId);
		assertThat(unlinked.chrecs()).isEmpty();
		assertThat(unlinked.status()).isEqualTo(ChangeSpecStatus.DRAFT);
	}

	@Test
	void emptyChangeSpecCanApplyAfterPendingBilling() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeSpecDto spec = createSpec(ownerFirmId);
		submitPendingBillingWithChrec(spec.changeSpecId());
		assertSpecValid(spec.changeSpecId());
		assertSpecApplySucceeds(spec.changeSpecId());
		assertThat(changeSpecs.find(spec.changeSpecId()).orElseThrow().status())
				.isEqualTo(ChangeSpecStatus.APPLIED);
	}

	@Test
	void applyOrderCommitsDependentsBeforeParents() {
		Long dataCenterId = seedDataCenter(unique("DC"));
		Long cageId = seedCage(unique("Cage"), dataCenterId);
		Long ownerFirmId = seedFirm(unique("Owner"));

		ChangeDto terminateCage = stageTerminateCurrent("CAGE", cageId);
		ChangeDto terminateDataCenter = stageTerminateCurrent("DATA_CENTER", dataCenterId);

		ChangeSpecDto spec = createSpec(ownerFirmId);
		addToSpec(spec.changeSpecId(), terminateDataCenter.changeId());
		addToSpec(spec.changeSpecId(), terminateCage.changeId());
		submitPendingBillingWithChrec(spec.changeSpecId());

		assertThat(AssetApplyOrder.rank("CAGE"))
				.isLessThan(AssetApplyOrder.rank("DATA_CENTER"));
		assertSpecApplySucceeds(spec.changeSpecId());

		assertThat(cages.findCurrent(cageId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(dataCenters.findCurrent(dataCenterId).orElseThrow().status()).isEqualTo("Terminated");
	}

	@Test
	void wrongStatusTransitionRejected() {
		Long ownerFirmId = seedFirm(unique("Owner"));
		ChangeSpecDto spec = createSpec(ownerFirmId);

		assertThatThrownBy(() -> changeSpecs.apply(spec.changeSpecId(), appliedBy))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("PENDING_BILLING");

		submitPendingBillingWithChrec(spec.changeSpecId());
		assertThatThrownBy(() -> changeSpecs.submitPendingBilling(spec.changeSpecId()))
				.isInstanceOf(WorkflowException.class)
				.hasMessageContaining("DRAFT");
	}
}
