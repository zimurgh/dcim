package com.dcim.connectivity.marketdatafeedtype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class MarketDataFeedTypeServiceTests extends ChangeTestSupport {

	@Test
	void addsUpdatesAndTerminatesMarketDataFeedTypeThroughChangeWorkflow() {
		Long id = applyAdd(
				AssetType.MARKET_DATA_FEED_TYPE,
				json(Map.of("marketDataFeedTypeName", unique("FeedType"))))
				.assetIdentityId();

		assertThat(marketDataFeedTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Active");
		assertThat(marketDataFeedTypes.listCurrent()).extracting(MarketDataFeedTypeDto::marketDataFeedTypeId)
				.contains(id);

		applyUpdateCurrent(
				AssetType.MARKET_DATA_FEED_TYPE,
				id,
				json(Map.of("marketDataFeedTypeName", unique("FeedType-B"))));
		assertThat(marketDataFeedTypes.history(id)).hasSize(2);

		applyTerminateCurrent(AssetType.MARKET_DATA_FEED_TYPE, id);
		assertThat(marketDataFeedTypes.findCurrent(id).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(marketDataFeedTypes.history(id)).hasSize(3);
	}
}
