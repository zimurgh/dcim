package com.dcim.connectivity.marketdatafeed;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.organization.user.TestUsers;
import com.dcim.organization.user.UserHistoryRepository;
import com.dcim.organization.user.UserIdentityRepository;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeAction;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeService;
import com.dcim.workflow.ChangeStage;
import com.dcim.workflow.HistoryLinkRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketDataFeedServiceTests {

	@Autowired
	MarketDataFeedService feeds;

	@Autowired
	ChangeService changes;

	@Autowired
	UserIdentityRepository userIdentities;

	@Autowired
	UserHistoryRepository userHistory;

	Long appliedBy;

	@BeforeEach
	void seedUser() {
		appliedBy = TestUsers.seed(userIdentities, userHistory, "tester");
	}

	@Test
	void addsMarketDataFeedThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto applied = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", seed.crossConnectId(), seed.feedTypeId(), seed.ownerFirmId(),
						seed.billingFirmId(), null));
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).singleElement().satisfies(link -> {
			assertThat(link.role()).isEqualTo(HistoryLinkRole.CREATED);
			assertThat(link.assetType()).isEqualTo(AssetType.MARKET_DATA_FEED);
		});

		MarketDataFeedDto current = feeds.findCurrent(applied.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A");
		assertThat(current.crossConnectId()).isEqualTo(seed.crossConnectId());
		assertThat(current.marketDataFeedTypeId()).isEqualTo(seed.feedTypeId());
		assertThat(current.ownerFirmId()).isEqualTo(seed.ownerFirmId());
		assertThat(current.billingFirmId()).isEqualTo(seed.billingFirmId());
		assertThat(current.providerFirmId()).isNull();
		assertThat(current.action()).isEqualTo("ADD");
		assertThat(current.status()).isEqualTo("Active");
		assertThat(feeds.listCurrentByCrossConnect(seed.crossConnectId())).hasSize(1);
		assertThat(feeds.history(applied.assetIdentityId())).hasSize(1);
	}

	@Test
	void updatesMarketDataFeedThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		Long otherFeedTypeId = applyAdd(
				AssetType.MARKET_DATA_FEED_TYPE,
				"{\"marketDataFeedTypeName\":\"Top of Book\"}")
				.assetIdentityId();
		Long otherCrossConnectId = applyAdd(
				AssetType.CROSS_CONNECT,
				xcPayload("XC-2", seed.crossConnectTypeId(), seed.latencyId(), seed.speedId(), seed.ownerFirmId(),
						seed.billingFirmId()))
				.assetIdentityId();
		Long providerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"ProviderCo\"}").assetIdentityId();

		ChangeDto added = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", seed.crossConnectId(), seed.feedTypeId(), seed.ownerFirmId(),
						seed.billingFirmId(), null));
		MarketDataFeedDto before = feeds.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked(
				feedPayload("FEED-A2", otherCrossConnectId, otherFeedTypeId, seed.ownerFirmId(),
						seed.billingFirmId(), providerFirmId),
				"tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.MARKET_DATA_FEED,
				ChangeAction.UPDATE,
				added.assetIdentityId(),
				before.marketDataFeedHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Update");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Active");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		MarketDataFeedDto current = feeds.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A2");
		assertThat(current.crossConnectId()).isEqualTo(otherCrossConnectId);
		assertThat(current.marketDataFeedTypeId()).isEqualTo(otherFeedTypeId);
		assertThat(current.providerFirmId()).isEqualTo(providerFirmId);
		assertThat(current.action()).isEqualTo("UPDATE");
		assertThat(feeds.listCurrentByCrossConnect(seed.crossConnectId())).isEmpty();
		assertThat(feeds.listCurrentByCrossConnect(otherCrossConnectId)).hasSize(1);
		assertThat(feeds.history(added.assetIdentityId())).hasSize(2);
		assertThat(feeds.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	@Test
	void terminatesMarketDataFeedThroughChangeWorkflow() {
		Seed seed = seedDependencies();
		ChangeDto added = applyAdd(
				AssetType.MARKET_DATA_FEED,
				feedPayload("FEED-A", seed.crossConnectId(), seed.feedTypeId(), seed.ownerFirmId(),
						seed.billingFirmId(), null));
		MarketDataFeedDto before = feeds.findCurrent(added.assetIdentityId()).orElseThrow();

		ChangeDto draft = changes.createUntracked("{}", "tester");
		ChangeDto staged = changes.promoteToStaged(
				draft.changeId(),
				AssetType.MARKET_DATA_FEED,
				ChangeAction.TERMINATE,
				added.assetIdentityId(),
				before.marketDataFeedHistoryId(),
				null,
				"tester");
		assertThat(staged.stage()).isEqualTo(ChangeStage.STAGED);
		assertThat(staged.statusLabel()).isEqualTo("Pending Terminate");

		ChangeDto applied = changes.applyStaged(draft.changeId(), appliedBy);
		assertThat(applied.stage()).isEqualTo(ChangeStage.COMMITTED);
		assertThat(applied.statusLabel()).isEqualTo("Terminated");
		assertThat(applied.historyLinks()).extracting(ChangeDto.HistoryLinkDto::role)
				.containsExactly(HistoryLinkRole.CLOSED_PRIOR, HistoryLinkRole.CREATED);

		MarketDataFeedDto current = feeds.findCurrent(added.assetIdentityId()).orElseThrow();
		assertThat(current.status()).isEqualTo("Terminated");
		assertThat(current.action()).isEqualTo("TERMINATE");
		assertThat(current.marketDataFeedName()).isEqualTo("FEED-A");
		assertThat(feeds.history(added.assetIdentityId())).hasSize(2);
		assertThat(feeds.history(added.assetIdentityId()).getFirst().validTo()).isNotNull();
	}

	private Seed seedDependencies() {
		Long ownerFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"OwnerCo\"}").assetIdentityId();
		Long billingFirmId = applyAdd(AssetType.FIRM, "{\"firmName\":\"BillingCo\"}").assetIdentityId();
		Long latencyId = applyAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"Low Latency\",\"latencyType\":\"LL\"}")
				.assetIdentityId();
		Long speedId = applyAdd(
				AssetType.SPEED,
				"{\"speedName\":\"1 Gigabit\",\"speedType\":\"1G\"}")
				.assetIdentityId();
		Long feedTypeId = applyAdd(
				AssetType.MARKET_DATA_FEED_TYPE,
				"{\"marketDataFeedTypeName\":\"Depth\"}")
				.assetIdentityId();
		Long crossConnectTypeId = applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"Single Mode Fiber\"}")
				.assetIdentityId();
		Long crossConnectId = applyAdd(
				AssetType.CROSS_CONNECT,
				xcPayload("XC-1", crossConnectTypeId, latencyId, speedId, ownerFirmId, billingFirmId))
				.assetIdentityId();
		return new Seed(ownerFirmId, billingFirmId, latencyId, speedId, feedTypeId, crossConnectTypeId,
				crossConnectId);
	}

	private static String xcPayload(
			String name,
			Long crossConnectTypeId,
			Long latencyId,
			Long speedId,
			Long ownerFirmId,
			Long billingFirmId) {
		return "{\"crossConnectName\":\"" + name + "\",\"circuitId\":\"CKT-" + name
				+ "\",\"crossConnectTypeId\":" + crossConnectTypeId
				+ ",\"latencyId\":" + latencyId
				+ ",\"speedId\":" + speedId
				+ ",\"ownerFirmId\":" + ownerFirmId
				+ ",\"billingFirmId\":" + billingFirmId + "}";
	}

	private static String feedPayload(
			String name,
			Long crossConnectId,
			Long feedTypeId,
			Long ownerFirmId,
			Long billingFirmId,
			Long providerFirmId) {
		String payload = "{\"marketDataFeedName\":\"" + name + "\",\"crossConnectId\":" + crossConnectId
				+ ",\"marketDataFeedTypeId\":" + feedTypeId
				+ ",\"ownerFirmId\":" + ownerFirmId
				+ ",\"billingFirmId\":" + billingFirmId;
		if (providerFirmId != null) {
			payload += ",\"providerFirmId\":" + providerFirmId;
		}
		return payload + "}";
	}

	private ChangeDto applyAdd(AssetType assetType, String payload) {
		ChangeDto draft = changes.createUntracked(payload, "tester");
		changes.promoteToStaged(
				draft.changeId(),
				assetType,
				ChangeAction.ADD,
				null,
				null,
				null,
				"tester");
		return changes.applyStaged(draft.changeId(), appliedBy);
	}

	private record Seed(
			Long ownerFirmId,
			Long billingFirmId,
			Long latencyId,
			Long speedId,
			Long feedTypeId,
			Long crossConnectTypeId,
			Long crossConnectId) {
	}
}
