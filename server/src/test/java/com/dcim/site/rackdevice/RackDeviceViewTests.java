package com.dcim.site.rackdevice;

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
class RackDeviceViewTests {

	@Autowired
	ChangeService changes;

	@Autowired
	RackDeviceViewRepository deviceViews;

	@Test
	void flattensRackCageAndDataCenterNames() {
		ChangeDto dataCenter = applyAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"NY4\"}");
		ChangeDto cage = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}");
		ChangeDto rack = applyAdd(
				AssetType.RACK,
				"{\"rackName\":\"R01\",\"cageId\":" + cage.assetIdentityId() + "}");
		ChangeDto device = applyAdd(
				AssetType.RACK_DEVICE,
				"{\"rackDeviceName\":\"sw1\",\"rackId\":" + rack.assetIdentityId() + "}");

		RackDeviceView view = deviceViews.findCurrentByRackDeviceId(device.assetIdentityId()).orElseThrow();
		assertThat(view.getRackDeviceId()).isEqualTo(device.assetIdentityId());
		assertThat(view.getRackDeviceName()).isEqualTo("sw1");
		assertThat(view.getRackId()).isEqualTo(rack.assetIdentityId());
		assertThat(view.getRackName()).isEqualTo("R01");
		assertThat(view.getCageId()).isEqualTo(cage.assetIdentityId());
		assertThat(view.getCageName()).isEqualTo("Cage-A");
		assertThat(view.getDataCenterId()).isEqualTo(dataCenter.assetIdentityId());
		assertThat(view.getDataCenterName()).isEqualTo("NY4");
		assertThat(view.getStatus()).isEqualTo("Active");

		assertThat(deviceViews.findCurrentByRackId(rack.assetIdentityId()))
				.extracting(RackDeviceView::getRackDeviceName)
				.containsExactly("sw1");
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
