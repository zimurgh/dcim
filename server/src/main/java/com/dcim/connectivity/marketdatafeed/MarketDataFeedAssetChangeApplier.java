package com.dcim.connectivity.marketdatafeed;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.crossconnect.CrossConnectIdentityRepository;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeIdentity;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeIdentityRepository;
import com.dcim.organization.firm.FirmIdentity;
import com.dcim.organization.firm.FirmIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketDataFeedAssetChangeApplier
		extends AbstractAssetChangeApplier<MarketDataFeedIdentity, MarketDataFeedHistory> {

	private final CrossConnectIdentityRepository crossConnects;
	private final MarketDataFeedTypeIdentityRepository feedTypes;
	private final FirmIdentityRepository firms;

	MarketDataFeedAssetChangeApplier(
			MarketDataFeedIdentityRepository identities,
			MarketDataFeedHistoryRepository history,
			CrossConnectIdentityRepository crossConnects,
			MarketDataFeedTypeIdentityRepository feedTypes,
			FirmIdentityRepository firms,
			JsonPayloads payloads) {
		super(
				"MARKET_DATA_FEED",
				"market data feed",
				identities,
				history,
				payloads,
				MarketDataFeedIdentity::new,
				MarketDataFeedIdentity::getMarketDataFeedId,
				MarketDataFeedHistory::getMarketDataFeedId,
				MarketDataFeedHistory::getMarketDataFeedHistoryId);
		this.crossConnects = crossConnects;
		this.feedTypes = feedTypes;
		this.firms = firms;
	}

	@Override
	protected MarketDataFeedHistory createAdd(
			MarketDataFeedIdentity identity, JsonNode body, AssetApplyCommand command) {
		CrossConnectIdentity crossConnect = requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"));
		MarketDataFeedTypeIdentity feedType = requireFeedType(
				JsonPayloads.requiredLong(body, "marketDataFeedTypeId"));
		FirmIdentity owner = requireFirm(JsonPayloads.requiredLong(body, "ownerFirmId"), "owner");
		FirmIdentity billing = requireFirm(JsonPayloads.requiredLong(body, "billingFirmId"), "billing");
		FirmIdentity provider = optionalFirm(JsonPayloads.longOrNull(body, "providerFirmId"), "provider");
		return new MarketDataFeedHistory(
				identity,
				crossConnect,
				feedType,
				JsonPayloads.requiredText(body, "marketDataFeedName"),
				owner,
				billing,
				provider,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketDataFeedHistory createUpdate(
			MarketDataFeedHistory prior, JsonNode body, AssetApplyCommand command) {
		CrossConnectIdentity crossConnect = body.hasNonNull("crossConnectId")
				? requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"))
				: prior.getCrossConnectIdentity();
		MarketDataFeedTypeIdentity feedType = body.hasNonNull("marketDataFeedTypeId")
				? requireFeedType(JsonPayloads.requiredLong(body, "marketDataFeedTypeId"))
				: prior.getMarketDataFeedTypeIdentity();
		FirmIdentity owner = body.hasNonNull("ownerFirmId")
				? requireFirm(JsonPayloads.requiredLong(body, "ownerFirmId"), "owner")
				: prior.getOwnerFirmIdentity();
		FirmIdentity billing = body.hasNonNull("billingFirmId")
				? requireFirm(JsonPayloads.requiredLong(body, "billingFirmId"), "billing")
				: prior.getBillingFirmIdentity();
		FirmIdentity provider = body.has("providerFirmId")
				? optionalFirm(JsonPayloads.longOrNull(body, "providerFirmId"), "provider")
				: prior.getProviderFirmIdentity();
		return new MarketDataFeedHistory(
				prior.getMarketDataFeedIdentity(),
				crossConnect,
				feedType,
				JsonPayloads.requiredText(body, "marketDataFeedName"),
				owner,
				billing,
				provider,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected MarketDataFeedHistory createTerminate(MarketDataFeedHistory prior, AssetApplyCommand command) {
		return new MarketDataFeedHistory(
				prior.getMarketDataFeedIdentity(),
				prior.getCrossConnectIdentity(),
				prior.getMarketDataFeedTypeIdentity(),
				prior.getMarketDataFeedName(),
				prior.getOwnerFirmIdentity(),
				prior.getBillingFirmIdentity(),
				prior.getProviderFirmIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private CrossConnectIdentity requireCrossConnect(Long crossConnectId) {
		return crossConnects.findById(crossConnectId)
				.orElseThrow(() -> new AssetApplyException("Cross connect not found: " + crossConnectId));
	}

	private MarketDataFeedTypeIdentity requireFeedType(Long marketDataFeedTypeId) {
		return feedTypes.findById(marketDataFeedTypeId)
				.orElseThrow(() -> new AssetApplyException(
						"Market data feed type not found: " + marketDataFeedTypeId));
	}

	private FirmIdentity requireFirm(Long firmId, String role) {
		return firms.findById(firmId)
				.orElseThrow(() -> new AssetApplyException(role + " firm not found: " + firmId));
	}

	private FirmIdentity optionalFirm(Long firmId, String role) {
		if (firmId == null) {
			return null;
		}
		return requireFirm(firmId, role);
	}
}
