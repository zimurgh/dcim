package com.dcim.organization.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class ExchangeServiceTests extends ChangeTestSupport {

	@Test
	void addsExchangeThroughChangeWorkflow() {
		ChangeDto applied = applyAdd(
				AssetType.EXCHANGE,
				json(Map.of(
						"exchangeName", "Chicago Board Options Exchange",
						"exchangeCode", "CBOE",
						"exchangeAbbreviation", "CBOE",
						"exchangeType", "OPTIONS")));

		ExchangeDto current = exchanges.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.exchangeName()).isEqualTo("Chicago Board Options Exchange");
		assertThat(current.exchangeCode()).isEqualTo("CBOE");
		assertThat(current.exchangeAbbreviation()).isEqualTo("CBOE");
		assertThat(current.exchangeType()).isEqualTo(ExchangeType.OPTIONS);
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(current.validTo()).isNull();
		assertThat(exchanges.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesExchangeThroughChangeWorkflow() {
		ChangeDto added = applyAdd(
				AssetType.EXCHANGE,
				json(Map.of(
						"exchangeName", "Chicago Board Options Exchange",
						"exchangeCode", "CBOE",
						"exchangeAbbreviation", "CBOE",
						"exchangeType", "OPTIONS")));

		applyUpdateCurrent(
				AssetType.EXCHANGE,
				added.assetIdentityId(),
				json(Map.of(
						"exchangeName", "Cboe Options Exchange",
						"exchangeCode", "CBOE",
						"exchangeAbbreviation", "Cboe",
						"exchangeType", "OPTIONS")));

		ExchangeDto current = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.exchangeName()).isEqualTo("Cboe Options Exchange");
		assertThat(current.exchangeAbbreviation()).isEqualTo("Cboe");
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(exchanges.history(added.assetIdentityId())).hasSize(2);
		assertThat(exchanges.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesExchangeThroughChangeWorkflow() {
		ChangeDto added = applyAdd(
				AssetType.EXCHANGE,
				json(Map.of(
						"exchangeName", "Chicago Board Options Exchange",
						"exchangeCode", "CBOE",
						"exchangeAbbreviation", "CBOE",
						"exchangeType", "OPTIONS")));

		applyTerminateCurrent(AssetType.EXCHANGE, added.assetIdentityId());

		ExchangeDto current = exchanges.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.exchangeCode()).isEqualTo("CBOE");
		assertThat(exchanges.history(added.assetIdentityId())).hasSize(2);
		assertThat(exchanges.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}
}
