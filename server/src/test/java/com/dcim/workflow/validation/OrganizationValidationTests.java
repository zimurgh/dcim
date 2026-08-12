package com.dcim.workflow.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.dcim.asset.ValidationCodes;
import com.dcim.workflow.ChangeDto;

import org.junit.jupiter.api.Test;

class OrganizationValidationTests extends ValidationTestSupport {

	@Test
	void firmNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd("FIRM", "{\"firmName\":\"" + unique("Acme") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void firmNameClashBlocksAddAndApply() {
		String name = unique("Acme");
		seedFirm(name);

		ChangeDto staged = stageAdd("FIRM", "{\"firmName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void exchangeNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				"EXCHANGE",
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
				"EXCHANGE",
				"{\"exchangeName\":\"" + name + "\",\"exchangeCode\":\"X\""
						+ ",\"exchangeAbbreviation\":\"X\",\"exchangeType\":\"EQUITIES\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void terminateExchangeSucceedsWhenUnreferenced() {
		Long exchangeId = seedExchange(unique("NYSE"), "EQUITIES");

		ChangeDto staged = stageTerminateCurrent("EXCHANGE", exchangeId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void marketSegmentNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd(
				"MARKET_SEGMENT",
				"{\"marketSegmentName\":\"" + unique("Equities") + "\",\"marketSegmentType\":\"EQUITIES_INDEX\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void marketSegmentNameClashBlocksAddAndApply() {
		String name = unique("Equities");
		seedMarketSegment(name, "EQUITIES_INDEX");

		ChangeDto staged = stageAdd(
				"MARKET_SEGMENT",
				"{\"marketSegmentName\":\"" + name + "\",\"marketSegmentType\":\"AGRICULTURAL_FUTURES\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void userNameAddSucceedsWhenUnique() {
		ChangeDto staged = stageAdd("USER", "{\"userName\":\"" + unique("alice") + "\"}");
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void userNameClashBlocksAddAndApply() {
		String name = unique("alice");
		seedUser(name);

		ChangeDto staged = stageAdd("USER", "{\"userName\":\"" + name + "\"}");
		assertInvalid(staged.changeId(), ValidationCodes.NAME_CLASH);
		assertApplyBlocked(staged.changeId(), ValidationCodes.NAME_CLASH);
	}

	@Test
	void terminateFirmSucceedsWhenUnreferenced() {
		Long firmId = seedFirm(unique("Acme"));

		ChangeDto staged = stageTerminateCurrent("FIRM", firmId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateFirmBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		seedCrossConnect(unique("CKT"), deps);

		ChangeDto staged = stageTerminateCurrent("FIRM", deps.ownerFirmId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateFirmBlockedByActiveMarketDataFeed() {
		XcDeps deps = seedXcDeps();
		Long crossConnectId = seedCrossConnect(unique("CKT"), deps);
		Long feedTypeId = seedMarketDataFeedType(unique("FeedType"));
		seedMarketDataFeed(unique("FEED"), crossConnectId, feedTypeId, deps);

		ChangeDto staged = stageTerminateCurrent("FIRM", deps.billingFirmId());
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateMarketSegmentSucceedsWhenUnreferenced() {
		Long marketSegmentId = seedMarketSegment(unique("Equities"), "EQUITIES_INDEX");

		ChangeDto staged = stageTerminateCurrent("MARKET_SEGMENT", marketSegmentId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}

	@Test
	void terminateMarketSegmentBlockedByActiveCrossConnect() {
		XcDeps deps = seedXcDeps();
		Long marketSegmentId = seedMarketSegment(unique("Equities"), "EQUITIES_INDEX");
		applyAdd(
				"CROSS_CONNECT",
				"{\"crossConnectName\":\"XC-Seg\",\"circuitId\":\"" + unique("CKT") + "\""
						+ ",\"crossConnectTypeId\":" + deps.crossConnectTypeId()
						+ ",\"latencyId\":" + deps.latencyId()
						+ ",\"speedId\":" + deps.speedId()
						+ ",\"marketSegmentId\":" + marketSegmentId
						+ ",\"ownerFirmId\":" + deps.ownerFirmId()
						+ ",\"billingFirmId\":" + deps.billingFirmId() + "}");

		ChangeDto staged = stageTerminateCurrent("MARKET_SEGMENT", marketSegmentId);
		assertInvalid(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
		assertApplyBlocked(staged.changeId(), ValidationCodes.ACTIVE_REFERENCES);
	}

	@Test
	void terminateUserAllowedEvenAfterApplyingChanges() {
		Long historyAuthorUserId = seedUser(unique("author"));

		ChangeDto dataCenterAdd = stageAdd("DATA_CENTER", "{\"dataCenterName\":\"" + unique("NY") + "\"}");
		ChangeDto applied = changes.applyStaged(dataCenterAdd.changeId(), historyAuthorUserId);
		assertThat(applied.appliedBy()).isEqualTo(historyAuthorUserId);

		ChangeDto staged = stageTerminateCurrent("USER", historyAuthorUserId);
		assertValid(staged.changeId());
		assertApplySucceeds(staged.changeId());
	}
}
