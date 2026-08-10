package com.dcim.site.rack;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RackViewTests {

	@Autowired
	ChangeService changes;

	@Autowired
	RackViewRepository rackViews;

	@Test
	void flattensCageAndDataCenterNames() {
		ChangeDto dataCenter = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");
		ChangeDto cage = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}");
		ChangeDto rack = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cage.assetIdentityId() + "}");

		RackView view = rackViews.findCurrentByRackId(rack.assetIdentityId()).orElseThrow();
		assertThat(view.getRackId()).isEqualTo(rack.assetIdentityId());
		assertThat(view.getRackName()).isEqualTo("R01");
		assertThat(view.getCageId()).isEqualTo(cage.assetIdentityId());
		assertThat(view.getCageName()).isEqualTo("Cage-A");
		assertThat(view.getDataCenterId()).isEqualTo(dataCenter.assetIdentityId());
		assertThat(view.getDataCenterName()).isEqualTo("NY4");
		assertThat(view.getStatus()).isEqualTo("Active");

		assertThat(rackViews.findCurrentByCageId(cage.assetIdentityId()))
				.extracting(RackView::getRackName)
				.containsExactly("R01");
		assertThat(rackViews.findCurrentByDataCenterId(dataCenter.assetIdentityId()))
				.extracting(RackView::getCageName)
				.containsExactly("Cage-A");
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
		return changes.applyStaged(draft.changeId(), "tester");
	}
}
