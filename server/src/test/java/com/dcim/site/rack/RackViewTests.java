package com.dcim.site.rack;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RackViewTests extends ChangeTestSupport {

	@Autowired
	RackViewRepository rackViews;

	@Test
	void flattensCageAndDataCenterNames() {
		Long dataCenterId = seedDataCenter("NY4");
		Long cageId = seedCage("Cage-A", dataCenterId);
		Long rackId = seedRack("R01", cageId);

		RackView view = rackViews.findCurrentByRackId(rackId).orElseThrow();
		assertThat(view.getRackId()).isEqualTo(rackId);
		assertThat(view.getRackName()).isEqualTo("R01");
		assertThat(view.getCageId()).isEqualTo(cageId);
		assertThat(view.getCageName()).isEqualTo("Cage-A");
		assertThat(view.getDataCenterId()).isEqualTo(dataCenterId);
		assertThat(view.getDataCenterName()).isEqualTo("NY4");
		assertThat(view.getStatus()).isEqualTo("Active");

		assertThat(rackViews.findCurrentByCageId(cageId))
				.extracting(RackView::getRackName)
				.containsExactly("R01");
		assertThat(rackViews.findCurrentByDataCenterId(dataCenterId))
				.extracting(RackView::getCageName)
				.containsExactly("Cage-A");
	}
}
