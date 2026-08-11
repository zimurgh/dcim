package com.dcim.organization.marketsegment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class MarketSegmentServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesMarketSegmentThroughChangeWorkflow() {
		Long id = applyAdd(
				AssetType.MARKET_SEGMENT,
				json(Map.of("marketSegmentName", unique("Seg"), "marketSegmentType", "EQUITIES_INDEX")))
				.assetIdentityId();

		MarketSegmentDto current = marketSegments.findCurrent(id).orElseThrow();
		assertThat(current.marketSegmentType()).isEqualTo(MarketSegmentType.EQUITIES_INDEX);
		assertThat(marketSegments.listCurrent()).extracting(MarketSegmentDto::marketSegmentId).contains(id);

		applyUpdateCurrent(
				AssetType.MARKET_SEGMENT,
				id,
				json(Map.of("marketSegmentName", unique("Seg-B"), "marketSegmentType", "AGRICULTURAL_FUTURES")));
		assertThat(marketSegments.findCurrent(id).orElseThrow().marketSegmentType())
				.isEqualTo(MarketSegmentType.AGRICULTURAL_FUTURES);
		assertThat(marketSegments.history(id)).hasSize(2);

		applyTerminateCurrent(AssetType.MARKET_SEGMENT, id);
		assertThat(marketSegments.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(marketSegments.history(id)).hasSize(3);
	}
}
