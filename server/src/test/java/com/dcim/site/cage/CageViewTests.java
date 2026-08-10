package com.dcim.site.cage;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CageViewTests {

	@Autowired
	ChangeService changes;

	@Autowired
	CageViewRepository cageViews;

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
	void exposesDataCenterNameInsteadOfIdentity() {
		ChangeDto dataCenter = applyAdd(
				AssetType.DATA_CENTER,
				"{\"dataCenterName\":\"NY4\"}",
				null,
				null);
		ChangeDto cage = applyAdd(
				AssetType.CAGE,
				"{\"cageName\":\"Cage-A\",\"dataCenterId\":" + dataCenter.assetIdentityId() + "}",
				null,
				null);

		CageView view = cageViews.findCurrentByCageId(cage.assetIdentityId()).orElseThrow();
		assertThat(view.getCageId()).isEqualTo(cage.assetIdentityId());
		assertThat(view.getCageName()).isEqualTo("Cage-A");
		assertThat(view.getDataCenterId()).isEqualTo(dataCenter.assetIdentityId());
		assertThat(view.getDataCenterName()).isEqualTo("NY4");
		assertThat(view.getStatus()).isEqualTo("Active");
		assertThat(view.getAppliedBy()).isEqualTo(appliedBy);

		assertThat(cageViews.findCurrentCages()).extracting(CageView::getDataCenterName).contains("NY4");
		assertThat(cageViews.findCurrentByDataCenterId(dataCenter.assetIdentityId()))
				.extracting(CageView::getCageName)
				.containsExactly("Cage-A");
	}

	private ChangeDto applyAdd(AssetType assetType, String payload, Long assetIdentityId, Long baseHistoryId) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(
				draft.changeId(),
				assetType,
				ChangeAction.ADD,
				assetIdentityId,
				baseHistoryId,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}
}
