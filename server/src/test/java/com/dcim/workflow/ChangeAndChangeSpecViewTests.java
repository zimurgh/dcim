package com.dcim.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.validation.ValidationTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChangeAndChangeSpecViewTests extends ValidationTestSupport {

	@Autowired
	ChangeViewRepository changeViews;

	@Autowired
	ChangeSpecViewRepository changeSpecViews;

	@Test
	void changeSpecViewExposesOwnerFirmName() {
		String ownerName = unique("Owner");
		Long ownerFirmId = seedFirm(ownerName);
		ChangeSpecDto spec = createSpec(ownerFirmId);

		ChangeSpecView view = changeSpecViews.findById(spec.changeSpecId()).orElseThrow();
		assertThat(view.getOwnerFirmId()).isEqualTo(ownerFirmId);
		assertThat(view.getOwnerFirmName()).isEqualTo(ownerName);
		assertThat(view.getStatus()).isEqualTo(ChangeSpecStatus.DRAFT);

		assertThat(changeSpecs.find(spec.changeSpecId()).orElseThrow().ownerFirmName()).isEqualTo(ownerName);
		assertThat(changeSpecs.listForFirm(ownerFirmId))
				.extracting(ChangeSpecDto::ownerFirmName)
				.contains(ownerName);
	}

	@Test
	void changeViewUnifiesStagesWithStatusLabelsAndSpecMembership() {
		XcDeps deps = seedXcDeps();
		ChangeSpecDto spec = createSpec(deps.ownerFirmId());
		ChangeDto staged = stageAdd(
				AssetType.CAGE,
				json(Map.of("cageName", unique("Cage"), "dataCenterId", seedDataCenter(unique("DC")))));
		addToSpec(spec.changeSpecId(), staged.changeId());

		ChangeView view = changeViews.findById(staged.changeId()).orElseThrow();
		assertThat(view.getStage()).isEqualTo(ChangeStage.STAGED);
		assertThat(view.getStatusLabel()).isEqualTo("Pending Add");
		assertThat(view.getAssetType()).isEqualTo(AssetType.CAGE);
		assertThat(view.getAction()).isEqualTo(ChangeAction.ADD);
		assertThat(view.getChangeSpecId()).isEqualTo(spec.changeSpecId());
		assertThat(view.getBody()).isNotBlank();

		assertThat(changes.listAll())
				.extracting(ChangeDto::changeId)
				.contains(staged.changeId());
		assertThat(changes.listAll().stream()
				.filter(c -> c.changeId().equals(staged.changeId()))
				.findFirst()
				.orElseThrow()
				.changeSpecId()).isEqualTo(spec.changeSpecId());
	}

	@Test
	void changeViewIncludesUntrackedAndCommitted() {
		ChangeDto untracked = changes.createUntracked("{\"note\":\"draft\"}", "tester");
		ChangeView draft = changeViews.findById(untracked.changeId()).orElseThrow();
		assertThat(draft.getStage()).isEqualTo(ChangeStage.UNTRACKED);
		assertThat(draft.getStatusLabel()).isEqualTo("Draft");
		assertThat(draft.getAssetType()).isNull();
		assertThat(draft.getAction()).isNull();

		Long dataCenterId = seedDataCenter(unique("DC"));
		ChangeDto committed = applyAdd(
				AssetType.CAGE,
				json(Map.of("cageName", unique("Cage"), "dataCenterId", dataCenterId)));
		ChangeView applied = changeViews.findById(committed.changeId()).orElseThrow();
		assertThat(applied.getStage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.getStatusLabel()).isEqualTo("Active");
		assertThat(applied.getAppliedBy()).isEqualTo(appliedBy);
		assertThat(applied.getAppliedByName()).isNotBlank();
	}
}
