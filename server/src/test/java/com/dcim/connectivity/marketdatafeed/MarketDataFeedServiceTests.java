package com.dcim.connectivity.marketdatafeed;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeTestSupport;

import org.junit.jupiter.api.Test;

class MarketDataFeedServiceTests extends ChangeTestSupport {

	@Test
	void addsMarketDataFeedThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);

		ChangeDto applied = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", crossConnectId, feedTypeId, deps, null));

		MarketDataFeedDto current = marketDataFeeds.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A");
		assertThat(current.crossConnectId()).isEqualTo(crossConnectId);
		assertThat(current.marketDataFeedTypeId()).isEqualTo(feedTypeId);
		assertThat(current.ownerFirmId()).isEqualTo(deps.ownerFirmId());
		assertThat(current.billingFirmId()).isEqualTo(deps.billingFirmId());
		assertThat(current.providerFirmId()).isNull();
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(marketDataFeeds.listCurrentByCrossConnect(crossConnectId)).hasSize(1);
		assertThat(marketDataFeeds.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesMarketDataFeedThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long otherFeedTypeId = seedMarketDataFeedType(unique("TopOfBook"));
		Long otherCrossConnectId = seedCrossConnect(unique("CKT2"), deps);
		Long providerFirmId = seedFirm(unique("Provider"));

		ChangeDto added = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", crossConnectId, feedTypeId, deps, null));

		applyUpdateCurrent(
				AssetType.MARKET_DATA_FEED,
				added.assetIdentityId(),
				feedPayload("FEED-A2", otherCrossConnectId, otherFeedTypeId, deps, providerFirmId));

		MarketDataFeedDto current = marketDataFeeds.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A2");
		assertThat(current.crossConnectId()).isEqualTo(otherCrossConnectId);
		assertThat(current.marketDataFeedTypeId()).isEqualTo(otherFeedTypeId);
		assertThat(current.providerFirmId()).isEqualTo(providerFirmId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(marketDataFeeds.listCurrentByCrossConnect(crossConnectId)).isEmpty();
		assertThat(marketDataFeeds.listCurrentByCrossConnect(otherCrossConnectId)).hasSize(1);
		assertThat(marketDataFeeds.history(added.assetIdentityId())).hasSize(2);
		assertThat(marketDataFeeds.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesMarketDataFeedThroughChangeWorkflow() {
		XcDeps deps = seedXcDeps();
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);

		ChangeDto added = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", crossConnectId, feedTypeId, deps, null));

		applyTerminateCurrent(AssetType.MARKET_DATA_FEED, added.assetIdentityId());

		MarketDataFeedDto current = marketDataFeeds.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A");
		assertThat(marketDataFeeds.history(added.assetIdentityId())).hasSize(2);
		assertThat(marketDataFeeds.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private String feedPayload(
			String name,
			Long crossConnectId,
			Long feedTypeId,
			XcDeps deps,
			Long providerFirmId) {
		return json(fields(
				"marketDataFeedName", name,
				"crossConnectId", crossConnectId,
				"marketDataFeedTypeId", feedTypeId,
				"ownerFirmId", deps.ownerFirmId(),
				"billingFirmId", deps.billingFirmId(),
				"providerFirmId", providerFirmId));
	}
}
