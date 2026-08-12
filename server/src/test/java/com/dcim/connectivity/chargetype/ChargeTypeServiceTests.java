package com.dcim.connectivity.chargetype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class ChargeTypeServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesChargeTypeThroughChangeWorkflow() {
		Long id = applyAdd("CHARGE_TYPE", json(Map.of("chargeTypeName", unique("MRC"))))
				.assetIdentityId();

		assertThat(chargeTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Active");
		assertThat(chargeTypes.listCurrent()).extracting(ChargeTypeDto::chargeTypeId).contains(id);

		applyUpdateCurrent("CHARGE_TYPE", id, json(Map.of("chargeTypeName", unique("NRC"))));
		assertThat(chargeTypes.history(id)).hasSize(2);

		applyTerminateCurrent("CHARGE_TYPE", id);
		assertThat(chargeTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(chargeTypes.history(id)).hasSize(3);
	}
}
