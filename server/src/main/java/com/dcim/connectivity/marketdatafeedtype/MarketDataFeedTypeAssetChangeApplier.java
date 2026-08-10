package com.dcim.connectivity.marketdatafeedtype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;
import com.dcim.connectivity.chargetype.ChargeTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketDataFeedTypeAssetChangeApplier
		extends AbstractAssetChangeApplier<MarketDataFeedTypeIdentity, MarketDataFeedTypeHistory> {

	private final ChargeTypeIdentityRepository chargeTypes;

	MarketDataFeedTypeAssetChangeApplier(
			MarketDataFeedTypeIdentityRepository identities,
			MarketDataFeedTypeHistoryRepository history,
			ChargeTypeIdentityRepository chargeTypes,
			JsonPayloads payloads) {
		super(
				"MARKET_DATA_FEED_TYPE",
				"market data feed type",
				identities,
				history,
				payloads,
				MarketDataFeedTypeIdentity::new,
				MarketDataFeedTypeIdentity::getMarketDataFeedTypeId,
				MarketDataFeedTypeHistory::getMarketDataFeedTypeId,
				MarketDataFeedTypeHistory::getMarketDataFeedTypeHistoryId);
		this.chargeTypes = chargeTypes;
	}

	@Override
	protected MarketDataFeedTypeHistory createAdd(
			MarketDataFeedTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new MarketDataFeedTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "marketDataFeedTypeName"),
				optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId")),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketDataFeedTypeHistory createUpdate(
			MarketDataFeedTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		ChargeTypeIdentity chargeType = body.has("chargeTypeId")
				? optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"))
				: prior.getChargeTypeIdentity();
		return new MarketDataFeedTypeHistory(
				prior.getMarketDataFeedTypeIdentity(),
				JsonPayloads.requiredText(body, "marketDataFeedTypeName"),
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketDataFeedTypeHistory createTerminate(
			MarketDataFeedTypeHistory prior, AssetApplyCommand command) {
		return new MarketDataFeedTypeHistory(
				prior.getMarketDataFeedTypeIdentity(),
				prior.getMarketDataFeedTypeName(),
				prior.getChargeTypeIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private ChargeTypeIdentity optionalChargeType(Long chargeTypeId) {
		if (chargeTypeId == null) {
			return null;
		}
		return chargeTypes.findById(chargeTypeId)
				.orElseThrow(() -> new AssetApplyException("Charge type not found: " + chargeTypeId));
	}
}
