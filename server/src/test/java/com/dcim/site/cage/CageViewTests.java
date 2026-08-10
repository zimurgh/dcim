package com.dcim.site.cage;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CageViewTests extends ChangeTestSupport {

	@Autowired
	CageViewRepository cageViews;

	@Test
	void exposesDataCenterNameInsteadOfIdentity() {
		Long dataCenterId = seedDataCenter("NY4");
		Long cageId = seedCage("Cage-A", dataCenterId);

		CageView view = cageViews.findCurrentByCageId(cageId).orElseThrow();
		assertThat(view.getCageId()).isEqualTo(cageId);
		assertThat(view.getCageName()).isEqualTo("Cage-A");
		assertThat(view.getDataCenterId()).isEqualTo(dataCenterId);
		assertThat(view.getDataCenterName()).isEqualTo("NY4");
		assertThat(view.getStatus()).isEqualTo("Active");
		assertThat(view.getAppliedBy()).isEqualTo(appliedBy);

		assertThat(cageViews.findCurrentCages()).extracting(CageView::getDataCenterName).contains("NY4");
		assertThat(cageViews.findCurrentByDataCenterId(dataCenterId))
				.extracting(CageView::getCageName)
				.containsExactly("Cage-A");
	}
}
