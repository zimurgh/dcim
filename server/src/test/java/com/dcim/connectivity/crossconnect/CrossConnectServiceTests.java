package com.dcim.connectivity.crossconnect;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class CrossConnectServiceTests extends ChangeTestSupport {

	@Test
	void addsCrossConnectThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long providerFirmId = seedFirm(unique("Provider"));
		ChangeDto applied = applyAdd("CROSS_CONNECT", xcWithProvider("XC-1", "CKT-XC-1", deps, providerFirmId));

		CrossConnectDto current = crossConnects.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectName()).isEqualTo("XC-1");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1");
		assertThat(current.crossConnectTypeId()).isEqualTo(deps.crossConnectTypeId());
		assertThat(current.latencyId()).isEqualTo(deps.latencyId());
		assertThat(current.speedId()).isEqualTo(deps.speedId());
		assertThat(current.ownerFirmId()).isEqualTo(deps.ownerFirmId());
		assertThat(current.billingFirmId()).isEqualTo(deps.billingFirmId());
		assertThat(current.providerFirmId()).isEqualTo(providerFirmId);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(crossConnects.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesCrossConnectThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long providerFirmId = seedFirm(unique("Provider"));
		Long otherBillingFirmId = seedFirm(unique("Billing2"));
		Long otherTypeId = seedCrossConnectType(unique("DarkFiber"));
		Long ullId = seedLatency(unique("ULL"), "ULL");
		Long speed10gId = seedSpeed(unique("10G"), "10G");

		ChangeDto added = applyAdd("CROSS_CONNECT", xcWithProvider("XC-1", "CKT-XC-1", deps, providerFirmId));

		applyUpdateCurrent(
				"CROSS_CONNECT",
				added.assetIdentityId(),
				json(fields(
						"crossConnectName", "XC-1-REN",
						"circuitId", "CKT-XC-1B",
						"crossConnectTypeId", otherTypeId,
						"latencyId", ullId,
						"speedId", speed10gId,
						"ownerFirmId", deps.ownerFirmId(),
						"billingFirmId", otherBillingFirmId,
						"providerFirmId", null)));

		CrossConnectDto current = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.crossConnectName()).isEqualTo("XC-1-REN");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1B");
		assertThat(current.crossConnectTypeId()).isEqualTo(otherTypeId);
		assertThat(current.latencyId()).isEqualTo(ullId);
		assertThat(current.speedId()).isEqualTo(speed10gId);
		assertThat(current.billingFirmId()).isEqualTo(otherBillingFirmId);
		assertThat(current.providerFirmId()).isNull();
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(crossConnects.history(added.assetIdentityId())).hasSize(2);
		assertThat(crossConnects.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesCrossConnectThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long providerFirmId = seedFirm(unique("Provider"));
		ChangeDto added = applyAdd("CROSS_CONNECT", xcWithProvider("XC-1", "CKT-XC-1", deps, providerFirmId));

		applyTerminateCurrent("CROSS_CONNECT", added.assetIdentityId());

		CrossConnectDto current = crossConnects.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.crossConnectName()).isEqualTo("XC-1");
		assertThat(current.circuitId()).isEqualTo("CKT-XC-1");
		assertThat(current.crossConnectTypeId()).isEqualTo(deps.crossConnectTypeId());
		assertThat(crossConnects.history(added.assetIdentityId())).hasSize(2);
		assertThat(crossConnects.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private String xcWithProvider(String name, String circuitId, XcDeps deps, Long providerFirmId) {
		return json(fields(
				"crossConnectName", name,
				"circuitId", circuitId,
				"crossConnectTypeId", deps.crossConnectTypeId(),
				"latencyId", deps.latencyId(),
				"speedId", deps.speedId(),
				"ownerFirmId", deps.ownerFirmId(),
				"billingFirmId", deps.billingFirmId(),
				"providerFirmId", providerFirmId));
	}
}
