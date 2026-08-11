package com.dcim.connectivity.marketdatafeed;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MarketDataFeedViewTests extends ChangeTestSupport {

	@Autowired
	MarketDataFeedViewRepository marketDataFeedViews;

	@Test
	void exposesFlattenedAssociationNames() {
		String feedTypeName = unique("FeedType");
		String feedName = unique("Feed");

		XcDeps deps = seedXcDeps();
		String ownerName = firms.findCurrent(deps.ownerFirmId()).orElseThrow().firmName();
		String billingName = firms.findCurrent(deps.billingFirmId()).orElseThrow().firmName();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(feedTypeName);
		Long feedId = seedMarketDataFeed(feedName, crossConnectId, feedTypeId, deps);

		MarketDataFeedView view = marketDataFeedViews.findCurrentByMarketDataFeedId(feedId).orElseThrow();
		assertThat(view.getMarketDataFeedId()).isEqualTo(feedId);
		assertThat(view.getMarketDataFeedName()).isEqualTo(feedName);
		assertThat(view.getCrossConnectId()).isEqualTo(crossConnectId);
		assertThat(view.getCrossConnectName()).startsWith("XC-");
		assertThat(view.getMarketDataFeedTypeId()).isEqualTo(feedTypeId);
		assertThat(view.getMarketDataFeedTypeName()).isEqualTo(feedTypeName);
		assertThat(view.getOwnerFirmId()).isEqualTo(deps.ownerFirmId());
		assertThat(view.getOwnerFirmName()).isEqualTo(ownerName);
		assertThat(view.getBillingFirmId()).isEqualTo(deps.billingFirmId());
		assertThat(view.getBillingFirmName()).isEqualTo(billingName);
		assertThat(view.getProviderFirmId()).isNull();
		assertThat(view.getProviderFirmName()).isNull();

		MarketDataFeedDto dto = marketDataFeeds.findCurrent(feedId).orElseThrow();
		assertThat(dto.ownerFirmName()).isEqualTo(ownerName);
		assertThat(dto.billingFirmName()).isEqualTo(billingName);
		assertThat(dto.marketDataFeedTypeName()).isEqualTo(feedTypeName);
		assertThat(dto.crossConnectName()).isNotBlank();

		assertThat(marketDataFeedViews.findCurrentByCrossConnectId(crossConnectId))
				.extracting(MarketDataFeedView::getMarketDataFeedName)
				.containsExactly(feedName);
	}
}
