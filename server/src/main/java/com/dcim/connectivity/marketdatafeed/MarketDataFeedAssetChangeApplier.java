package com.dcim.connectivity.marketdatafeed;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
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
class MarketDataFeedAssetChangeApplier implements AssetChangeApplier {

	private final MarketDataFeedIdentityRepository identities;
	private final MarketDataFeedHistoryRepository history;
	private final CrossConnectIdentityRepository crossConnects;
	private final MarketDataFeedTypeIdentityRepository feedTypes;
	private final FirmIdentityRepository firms;
	private final JsonPayloads payloads;

	MarketDataFeedAssetChangeApplier(
			MarketDataFeedIdentityRepository identities,
			MarketDataFeedHistoryRepository history,
			CrossConnectIdentityRepository crossConnects,
			MarketDataFeedTypeIdentityRepository feedTypes,
			FirmIdentityRepository firms,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.crossConnects = crossConnects;
		this.feedTypes = feedTypes;
		this.firms = firms;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "MARKET_DATA_FEED".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported market data feed action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketDataFeedName");
		CrossConnectIdentity crossConnect = requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"));
		MarketDataFeedTypeIdentity feedType = requireFeedType(
				JsonPayloads.requiredLong(body, "marketDataFeedTypeId"));
		FirmIdentity owner = requireFirm(JsonPayloads.requiredLong(body, "ownerFirmId"), "owner");
		FirmIdentity billing = requireFirm(JsonPayloads.requiredLong(body, "billingFirmId"), "billing");
		FirmIdentity provider = optionalFirm(JsonPayloads.longOrNull(body, "providerFirmId"), "provider");
		MarketDataFeedIdentity identity = identities.saveAndFlush(new MarketDataFeedIdentity());
		MarketDataFeedHistory created = history.saveAndFlush(new MarketDataFeedHistory(
				identity,
				crossConnect,
				feedType,
				name,
				owner,
				billing,
				provider,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getMarketDataFeedId(),
				List.of(new AssetHistoryLink(created.getMarketDataFeedHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		MarketDataFeedHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketDataFeedName");
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
		prior.close(command.validOn());
		MarketDataFeedHistory created = history.saveAndFlush(new MarketDataFeedHistory(
				prior.getMarketDataFeedIdentity(),
				crossConnect,
				feedType,
				name,
				owner,
				billing,
				provider,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		MarketDataFeedHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		MarketDataFeedHistory created = history.saveAndFlush(new MarketDataFeedHistory(
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
				command.committedStatus()));
		return result(prior, created);
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

	private MarketDataFeedHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Market data feed update/terminate requires assetIdentityId and baseHistoryId");
		}
		MarketDataFeedHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Market data feed history not found: " + command.baseHistoryId()));
		if (!prior.getMarketDataFeedId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to market data feed " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale market data feed baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(MarketDataFeedHistory prior, MarketDataFeedHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getMarketDataFeedHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getMarketDataFeedHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getMarketDataFeedId(), List.copyOf(links));
	}
}
