package com.dcim.organization.marketsegment;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketSegmentAssetChangeApplier
		extends AbstractAssetChangeApplier<MarketSegmentIdentity, MarketSegmentHistory> {

	MarketSegmentAssetChangeApplier(
			MarketSegmentIdentityRepository identities,
			MarketSegmentHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"MARKET_SEGMENT",
				"market segment",
				identities,
				history,
				payloads,
				MarketSegmentIdentity::new,
				MarketSegmentIdentity::getMarketSegmentId,
				MarketSegmentHistory::getMarketSegmentId,
				MarketSegmentHistory::getMarketSegmentHistoryId);
	}

	@Override
	protected MarketSegmentHistory createAdd(
			MarketSegmentIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new MarketSegmentHistory(
				identity,
				JsonPayloads.requiredText(body, "marketSegmentName"),
				requireMarketSegmentType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketSegmentHistory createUpdate(
			MarketSegmentHistory prior, JsonNode body, AssetApplyCommand command) {
		return new MarketSegmentHistory(
				prior.getMarketSegmentIdentity(),
				JsonPayloads.requiredText(body, "marketSegmentName"),
				requireMarketSegmentType(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketSegmentHistory createTerminate(MarketSegmentHistory prior, AssetApplyCommand command) {
		return new MarketSegmentHistory(
				prior.getMarketSegmentIdentity(),
				prior.getMarketSegmentName(),
				prior.getMarketSegmentType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private static MarketSegmentType requireMarketSegmentType(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "marketSegmentType");
		try {
			return MarketSegmentType.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException(
					"marketSegmentType must be Equities Index or Agricultural Futures: " + raw);
		}
	}
}
