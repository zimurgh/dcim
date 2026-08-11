package com.dcim.connectivity.crossconnect;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CrossConnectViewTests extends ChangeTestSupport {

	@Autowired
	CrossConnectViewRepository crossConnectViews;

	@Test
	void exposesFlattenedAssociationNames() {
		String ownerName = unique("Owner");
		String billingName = unique("Billing");
		String providerName = unique("Provider");
		String typeName = unique("XcType");
		String latencyName = unique("Latency");
		String speedName = unique("Speed");

		Long ownerFirmId = seedFirm(ownerName);
		Long billingFirmId = seedFirm(billingName);
		Long providerFirmId = seedFirm(providerName);
		Long typeId = seedCrossConnectType(typeName);
		Long latencyId = seedLatency(latencyName, "LL");
		Long speedId = seedSpeed(speedName, "1G");

		Long crossConnectId = applyAdd(
				com.dcim.workflow.AssetType.CROSS_CONNECT,
				json(fields(
						"crossConnectName", "XC-Flat",
						"circuitId", unique("CKT"),
						"crossConnectTypeId", typeId,
						"latencyId", latencyId,
						"speedId", speedId,
						"ownerFirmId", ownerFirmId,
						"billingFirmId", billingFirmId,
						"providerFirmId", providerFirmId)))
				.assetIdentityId();

		CrossConnectView view = crossConnectViews.findCurrentByCrossConnectId(crossConnectId).orElseThrow();
		assertThat(view.getCrossConnectId()).isEqualTo(crossConnectId);
		assertThat(view.getCrossConnectName()).isEqualTo("XC-Flat");
		assertThat(view.getCrossConnectTypeId()).isEqualTo(typeId);
		assertThat(view.getCrossConnectTypeName()).isEqualTo(typeName);
		assertThat(view.getLatencyId()).isEqualTo(latencyId);
		assertThat(view.getLatencyName()).isEqualTo(latencyName);
		assertThat(view.getSpeedId()).isEqualTo(speedId);
		assertThat(view.getSpeedName()).isEqualTo(speedName);
		assertThat(view.getOwnerFirmId()).isEqualTo(ownerFirmId);
		assertThat(view.getOwnerFirmName()).isEqualTo(ownerName);
		assertThat(view.getBillingFirmId()).isEqualTo(billingFirmId);
		assertThat(view.getBillingFirmName()).isEqualTo(billingName);
		assertThat(view.getProviderFirmId()).isEqualTo(providerFirmId);
		assertThat(view.getProviderFirmName()).isEqualTo(providerName);
		assertThat(view.getMarketSegmentId()).isNull();
		assertThat(view.getMarketSegmentName()).isNull();

		CrossConnectDto dto = crossConnects.findCurrent(crossConnectId).orElseThrow();
		assertThat(dto.ownerFirmName()).isEqualTo(ownerName);
		assertThat(dto.billingFirmName()).isEqualTo(billingName);
		assertThat(dto.providerFirmName()).isEqualTo(providerName);
		assertThat(dto.crossConnectTypeName()).isEqualTo(typeName);
		assertThat(dto.latencyName()).isEqualTo(latencyName);
		assertThat(dto.speedName()).isEqualTo(speedName);

		assertThat(crossConnectViews.findCurrent())
				.extracting(CrossConnectView::getCrossConnectName)
				.contains("XC-Flat");
	}
}
