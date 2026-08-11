package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;
import com.dcim.workflow.ChangeSpecDto;

import org.junit.jupiter.api.Test;

class ConnectivityValidationTests extends ValidationTestSupport {

	@Test
	void circuitIdUniqueAllowsAdd() {
		XcDeps deps = seedXcDeps();
		ChangeDto staged = stageAdd(AssetType.CROSS_CONNECT, xcPayload(unique("CKT"), deps));
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void circuitIdClashBlocksSecondCrossConnect() {
		XcDeps deps = seedXcDeps();
		String circuitId = unique("CKT");
		seedCrossConnect(circuitId, deps);

		ChangeDto staged = stageAdd(AssetType.CROSS_CONNECT, xcPayload(circuitId, deps));
		assertInvalid(staged.changeId(), ValidationCodes.VALUE_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.VALUE_CLASH);
	}

	@Test
	void marketDataFeedNameUniqueWithinCrossConnectAllowsAdd() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));

		ChangeDto staged = stageAdd(
				AssetType.MARKET_DATA_FEED,
				"{\"marketDataFeedName\":\"" + unique("FEED") + "\",\"crossConnectId\":" + crossConnectId
						+ ",\"marketDataFeedTypeId\":" + feedTypeId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void marketDataFeedNameClashWithinSameCrossConnectBlocksAddAndApply() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		String feedName = unique("FEED");
		seedMarketDataFeed(feedName, crossConnectId, feedTypeId, deps);

		ChangeDto staged = stageAdd(
				AssetType.MARKET_DATA_FEED,
				"{\"marketDataFeedName\":\"" + feedName + "\",\"crossConnectId\":" + crossConnectId
						+ ",\"marketDataFeedTypeId\":" + feedTypeId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void marketDataFeedNameAllowedAcrossDifferentCrossConnects() {
		XcDeps deps = seedXcDeps();
		Long xc1 = seedCrossConnect(unique("CKT"), deps);
		Long xc2 = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		String feedName = unique("FEED");
		seedMarketDataFeed(feedName, xc1, feedTypeId, deps);

		ChangeDto staged = stageAdd(
				AssetType.MARKET_DATA_FEED,
				"{\"marketDataFeedName\":\"" + feedName + "\",\"crossConnectId\":" + xc2
						+ ",\"marketDataFeedTypeId\":" + feedTypeId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void documentNameUniqueWithinCrossConnectAllowsAdd() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageAdd(
				AssetType.DOCUMENT, "{\"documentName\":\"" + unique("LOA") + "\",\"crossConnectId\":" + crossConnectId + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void documentNameClashWithinSameCrossConnectBlocksAddAndApply() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		String documentName = unique("LOA");
		seedDocument(documentName, crossConnectId);

		ChangeDto staged = stageAdd(
				AssetType.DOCUMENT, "{\"documentName\":\"" + documentName + "\",\"crossConnectId\":" + crossConnectId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void documentNameAllowedAcrossDifferentCrossConnects() {
		XcDeps deps = seedXcDeps();
		Long xc1 = seedCrossConnect(unique("CKT"), deps);
		Long xc2 = seedCrossConnect(unique("CKT"), deps);
		String documentName = unique("LOA");
		seedDocument(documentName, xc1);

		ChangeDto staged = stageAdd(
				AssetType.DOCUMENT, "{\"documentName\":\"" + documentName + "\",\"crossConnectId\":" + xc2 + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void cableWithDistinctPortsAllowsAdd() {
		Long[] ports = seedPortPair();
		ChangeDto staged = stageAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + ports[1] + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void cableWithSamePortTwiceBlocksAddAndApply() {
		Long[] ports = seedPortPair();
		ChangeDto staged = stageAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + ports[0] + "}");
		assertInvalid(staged.changeId(), ValidationCodes.VALUE_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.VALUE_CLASH);
	}

	@Test
	void cablePortOccupancyAllowsAddWhenPortsFree() {
		Long[] ports = seedPortPair();
		ChangeDto staged = stageAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + ports[1] + "}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void cablePortAlreadyUsedByActiveCableBlocksAddAndApply() {
		Long[] ports = seedPortPair();
		seedCable(unique("CBL"), ports[0], ports[1]);
		SiteDeviceFixture device = seedDeviceInNewTree();
		Long portTypeId = seedRackDevicePortType(unique("PortType"));
		Long spareId = seedRackDevicePort(unique("eth"), device.rackDeviceId(), portTypeId);

		ChangeDto staged = stageAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + spareId + "}");
		assertInvalid(staged.changeId(), ValidationCodes.VALUE_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.VALUE_CLASH);
	}

	@Test
	void latencyTypeUniqueAllowsAddForUnusedType() {
		seedLatency(unique("Latency"), "LL");
		ChangeDto staged = stageAdd(AssetType.LATENCY, "{\"latencyName\":\"" + unique("Latency") + "\",\"latencyType\":\"ULL\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void latencyTypeClashBlocksSecondLatencyOfSameType() {
		seedLatency(unique("Latency"), "LL");
		ChangeDto staged = stageAdd(AssetType.LATENCY, "{\"latencyName\":\"" + unique("Latency") + "\",\"latencyType\":\"LL\"}");
		assertInvalid(staged.changeId(), ValidationCodes.VALUE_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.VALUE_CLASH);
	}

	@Test
	void latencyNameUniqueAllowsAdd() {
		seedLatency(unique("Latency"), "LL");
		ChangeDto staged = stageAdd(
				AssetType.LATENCY,
				"{\"latencyName\":\"" + unique("OtherLatency") + "\",\"latencyType\":\"ULL\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void latencyNameClashBlocksAddAndApply() {
		String name = unique("Latency");
		seedLatency(name, "LL");
		ChangeDto staged = stageAdd(AssetType.LATENCY, "{\"latencyName\":\"" + name + "\",\"latencyType\":\"ULL\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void speedTypeUniqueAllowsAddForUnusedType() {
		seedSpeed(unique("Speed"), "1G");
		ChangeDto staged = stageAdd(AssetType.SPEED, "{\"speedName\":\"" + unique("Speed") + "\",\"speedType\":\"10G\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void speedTypeClashBlocksSecondSpeedOfSameType() {
		seedSpeed(unique("Speed"), "1G");
		ChangeDto staged = stageAdd(AssetType.SPEED, "{\"speedName\":\"" + unique("Speed") + "\",\"speedType\":\"1G\"}");
		assertInvalid(staged.changeId(), ValidationCodes.VALUE_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.VALUE_CLASH);
	}

	@Test
	void speedNameUniqueAllowsAdd() {
		seedSpeed(unique("Speed"), "1G");
		ChangeDto staged = stageAdd(
				AssetType.SPEED,
				"{\"speedName\":\"" + unique("OtherSpeed") + "\",\"speedType\":\"10G\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void speedNameClashBlocksAddAndApply() {
		String name = unique("Speed");
		seedSpeed(name, "1G");
		ChangeDto staged = stageAdd(AssetType.SPEED, "{\"speedName\":\"" + name + "\",\"speedType\":\"10G\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void chargeTypeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(AssetType.CHARGE_TYPE, "{\"chargeTypeName\":\"" + unique("MRC") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void chargeTypeNameClashBlocksAddAndApply() {
		String name = unique("MRC");
		seedChargeType(name);

		ChangeDto staged = stageAdd(AssetType.CHARGE_TYPE, "{\"chargeTypeName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void crossConnectTypeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.CROSS_CONNECT_TYPE, "{\"crossConnectTypeName\":\"" + unique("XcType") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void crossConnectTypeNameClashBlocksAddAndApply() {
		String name = unique("XcType");
		seedCrossConnectType(name);

		ChangeDto staged = stageAdd(AssetType.CROSS_CONNECT_TYPE, "{\"crossConnectTypeName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void marketDataFeedTypeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.MARKET_DATA_FEED_TYPE, "{\"marketDataFeedTypeName\":\"" + unique("FeedType") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void marketDataFeedTypeNameClashBlocksAddAndApply() {
		String name = unique("FeedType");
		seedMarketDataFeedType(name);

		ChangeDto staged = stageAdd(AssetType.MARKET_DATA_FEED_TYPE, "{\"marketDataFeedTypeName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void terminateCrossConnectSucceedsWhenNoLiveChildren() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT, crossConnectId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateCrossConnectBlockedByLiveMarketDataFeed() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		seedMarketDataFeed(unique("FEED"), crossConnectId, feedTypeId, deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT, crossConnectId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateCrossConnectBlockedByLiveDocument() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		seedDocument(unique("LOA"), crossConnectId);

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT, crossConnectId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateCrossConnectBlockedByLiveCable() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long[] ports = seedPortPair();
		applyAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + ports[1]
						+ ",\"crossConnectId\":" + crossConnectId + "}");

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT, crossConnectId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void batchTerminateCrossConnectWithFeedDocumentAndCableTogetherOnChangeSpecPasses() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		Long feedId = seedMarketDataFeed(unique("FEED"), crossConnectId, feedTypeId, deps);
		Long documentId = seedDocument(unique("LOA"), crossConnectId);
		Long[] ports = seedPortPair();
		Long cableId = applyAdd(
				AssetType.CABLE,
				"{\"cableName\":\"" + unique("CBL") + "\",\"portAId\":" + ports[0] + ",\"portBId\":" + ports[1]
						+ ",\"crossConnectId\":" + crossConnectId + "}")
				.assetIdentityId();

		ChangeDto terminateFeed = stageTerminateCurrent(AssetType.MARKET_DATA_FEED, feedId);
		ChangeDto terminateDocument = stageTerminateCurrent(AssetType.DOCUMENT, documentId);
		ChangeDto terminateCable = stageTerminateCurrent(AssetType.CABLE, cableId);
		ChangeDto terminateCrossConnect = stageTerminateCurrent(AssetType.CROSS_CONNECT, crossConnectId);

		ChangeSpecDto spec = createSpec(deps.ownerFirmId());
		addToSpec(spec.changeSpecId(), terminateFeed.changeId());
		addToSpec(spec.changeSpecId(), terminateDocument.changeId());
		addToSpec(spec.changeSpecId(), terminateCable.changeId());
		addToSpec(spec.changeSpecId(), terminateCrossConnect.changeId());
		submitPendingBillingWithChrec(spec.changeSpecId());

		assertSpecValid(spec.changeSpecId());
		assertSpecApplySucceeds(spec.changeSpecId());

		assertThat(crossConnects.findCurrent(crossConnectId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(marketDataFeeds.findCurrent(feedId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(documents.findCurrent(documentId).orElseThrow().status()).isEqualTo("Terminated");
		assertThat(cables.findCurrent(cableId).orElseThrow().status()).isEqualTo("Terminated");
	}

	@Test
	void terminateLatencyBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.LATENCY, deps.latencyId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateLatencySucceedsWhenUnreferenced() {
		Long latencyId = seedLatency(unique("Latency"), "LL");

		ChangeDto staged = stageTerminateCurrent(AssetType.LATENCY, latencyId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateSpeedBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.SPEED, deps.speedId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateSpeedSucceedsWhenUnreferenced() {
		Long speedId = seedSpeed(unique("Speed"), "1G");

		ChangeDto staged = stageTerminateCurrent(AssetType.SPEED, speedId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateCrossConnectTypeBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT_TYPE, deps.crossConnectTypeId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateCrossConnectTypeSucceedsWhenUnreferenced() {
		Long typeId = seedCrossConnectType(unique("XcType"));

		ChangeDto staged = stageTerminateCurrent(AssetType.CROSS_CONNECT_TYPE, typeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateMarketDataFeedTypeBlockedByActiveFeed() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		seedMarketDataFeed(unique("FEED"), crossConnectId, feedTypeId, deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.MARKET_DATA_FEED_TYPE, feedTypeId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_CHILDREN);
	}

	@Test
	void terminateMarketDataFeedTypeSucceedsWhenUnreferenced() {
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));

		ChangeDto staged = stageTerminateCurrent(AssetType.MARKET_DATA_FEED_TYPE, feedTypeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateChargeTypeBlockedByActiveCrossConnectType() {
		Long chargeTypeId = seedChargeType(unique("MRC"));
		applyAdd(
				AssetType.CROSS_CONNECT_TYPE,
				"{\"crossConnectTypeName\":\"" + unique("XcType") + "\",\"chargeTypeId\":" + chargeTypeId + "}");

		ChangeDto staged = stageTerminateCurrent(AssetType.CHARGE_TYPE, chargeTypeId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateChargeTypeBlockedByActiveMarketDataFeedType() {
		Long chargeTypeId = seedChargeType(unique("MRC"));
		applyAdd(
				AssetType.MARKET_DATA_FEED_TYPE,
				"{\"marketDataFeedTypeName\":\"" + unique("FeedType") + "\",\"chargeTypeId\":" + chargeTypeId + "}");

		ChangeDto staged = stageTerminateCurrent(AssetType.CHARGE_TYPE, chargeTypeId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateChargeTypeSucceedsWhenUnreferenced() {
		Long chargeTypeId = seedChargeType(unique("MRC"));

		ChangeDto staged = stageTerminateCurrent(AssetType.CHARGE_TYPE, chargeTypeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}
}
