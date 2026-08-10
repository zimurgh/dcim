package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.AssetType;
import com.dcim.workflow.ChangeDto;

import org.junit.jupiter.api.Test;

/**
 * Organization rules from DESIGN.md: Firm / Exchange / Market Segment / User name clashes, Firm and
 * Market Segment terminate guards, and the User exception (terminate allowed even after the user has
 * applied changes, since historical {@code appliedBy} never blocks).
 */
class OrganizationValidationTests extends ValidationTestSupport {

	// ================================================================== clash: Firm name (global)

	@Test
	void firmNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(AssetType.FIRM, "{\"firmName\":\"" + unique("Acme") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void firmNameClashBlocksAddAndApply() {
		String name = unique("Acme");
		seedFirm(name);

		ChangeDto staged = stageAdd(AssetType.FIRM, "{\"firmName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	// ================================================================== clash: Exchange name (global)

	@Test
	void exchangeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"" + unique("CBOE") + "\",\"exchangeCode\":\"CBOE\""
						+ ",\"exchangeAbbreviation\":\"CBOE\",\"exchangeType\":\"OPTIONS\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void exchangeNameClashBlocksAddAndApply() {
		String name = unique("CBOE");
		seedExchange(name, "OPTIONS");

		ChangeDto staged = stageAdd(
				AssetType.EXCHANGE,
				"{\"exchangeName\":\"" + name + "\",\"exchangeCode\":\"X\""
						+ ",\"exchangeAbbreviation\":\"X\",\"exchangeType\":\"EQUITIES\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void terminateExchangeSucceedsWhenUnreferenced() {
		Long exchangeId = seedExchange(unique("NYSE"), "EQUITIES");

		ChangeDto staged = stageTerminateCurrent(AssetType.EXCHANGE, exchangeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	// ================================================================== clash: Market Segment name (global)

	@Test
	void marketSegmentNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				AssetType.MARKET_SEGMENT,
				"{\"marketSegmentName\":\"" + unique("Equities") + "\",\"marketSegmentType\":\"EQUITIES_INDEX\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void marketSegmentNameClashBlocksAddAndApply() {
		String name = unique("Equities");
		seedMarketSegment(name, "EQUITIES_INDEX");

		ChangeDto staged = stageAdd(
				AssetType.MARKET_SEGMENT,
				"{\"marketSegmentName\":\"" + name + "\",\"marketSegmentType\":\"AGRICULTURAL_FUTURES\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	// ================================================================== clash: User name (global)

	@Test
	void userNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(AssetType.USER, "{\"userName\":\"" + unique("alice") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void userNameClashBlocksAddAndApply() {
		String name = unique("alice");
		seedUser(name);

		ChangeDto staged = stageAdd(AssetType.USER, "{\"userName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	// ================================================================== terminate: Firm <- Cross Connect / Feed

	@Test
	void terminateFirmSucceedsWhenUnreferenced() {
		Long firmId = seedFirm(unique("Acme"));

		ChangeDto staged = stageTerminateCurrent(AssetType.FIRM, firmId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateFirmBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.FIRM, deps.ownerFirmId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateFirmBlockedByActiveMarketDataFeed() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		seedMarketDataFeed(unique("FEED"), crossConnectId, feedTypeId, deps);

		ChangeDto staged = stageTerminateCurrent(AssetType.FIRM, deps.billingFirmId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	// ================================================================== terminate: Market Segment <- Cross Connect

	@Test
	void terminateMarketSegmentSucceedsWhenUnreferenced() {
		Long marketSegmentId = seedMarketSegment(unique("Equities"), "EQUITIES_INDEX");

		ChangeDto staged = stageTerminateCurrent(AssetType.MARKET_SEGMENT, marketSegmentId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateMarketSegmentBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		Long marketSegmentId = seedMarketSegment(unique("Equities"), "EQUITIES_INDEX");
		applyAdd(
				AssetType.CROSS_CONNECT,
				"{\"crossConnectName\":\"XC-Seg\",\"circuitId\":\"" + unique("CKT") + "\""
						+ ",\"crossConnectTypeId\":" + deps.crossConnectTypeId()
						+ ",\"latencyId\":" + deps.latencyId()
						+ ",\"speedId\":" + deps.speedId()
						+ ",\"marketSegmentId\":" + marketSegmentId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}");

		ChangeDto staged = stageTerminateCurrent(AssetType.MARKET_SEGMENT, marketSegmentId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	// ================================================================== User terminate: not blocked by history

	@Test
	void terminateUserAllowedEvenAfterApplyingChanges() {
		Long historyAuthorUserId = seedUser(unique("author"));

		// The seeded user is used as appliedBy on an unrelated change, i.e. it has authored history.
		ChangeDto dataCenterAdd = stageAdd(AssetType.DATA_CENTER, "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeDto applied = changes.applyStaged(dataCenterAdd.changeId(), historyAuthorUserId);
		assertThat(applied.appliedBy()).isEqualTo(historyAuthorUserId);

		ChangeDto staged = stageTerminateCurrent(AssetType.USER, historyAuthorUserId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}
}
