package com.dcim.connectivity.marketdatafeedtype;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;
import com.dcim.connectivity.chargetype.ChargeTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketDataFeedTypeAssetChangeApplier implements AssetChangeApplier {

	private final MarketDataFeedTypeIdentityRepository identities;
	private final MarketDataFeedTypeHistoryRepository history;
	private final ChargeTypeIdentityRepository chargeTypes;
	private final JsonPayloads payloads;

	MarketDataFeedTypeAssetChangeApplier(
			MarketDataFeedTypeIdentityRepository identities,
			MarketDataFeedTypeHistoryRepository history,
			ChargeTypeIdentityRepository chargeTypes,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.chargeTypes = chargeTypes;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "MARKET_DATA_FEED_TYPE".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported market data feed type action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketDataFeedTypeName");
		ChargeTypeIdentity chargeType = optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"));
		MarketDataFeedTypeIdentity identity = identities.saveAndFlush(new MarketDataFeedTypeIdentity());
		MarketDataFeedTypeHistory created = history.saveAndFlush(new MarketDataFeedTypeHistory(
				identity,
				name,
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getMarketDataFeedTypeId(),
				List.of(new AssetHistoryLink(
						created.getMarketDataFeedTypeHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		MarketDataFeedTypeHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketDataFeedTypeName");
		ChargeTypeIdentity chargeType = body.has("chargeTypeId")
				? optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"))
				: prior.getChargeTypeIdentity();
		prior.close(command.validOn());
		MarketDataFeedTypeHistory created = history.saveAndFlush(new MarketDataFeedTypeHistory(
				prior.getMarketDataFeedTypeIdentity(),
				name,
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		MarketDataFeedTypeHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		MarketDataFeedTypeHistory created = history.saveAndFlush(new MarketDataFeedTypeHistory(
				prior.getMarketDataFeedTypeIdentity(),
				prior.getMarketDataFeedTypeName(),
				prior.getChargeTypeIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private ChargeTypeIdentity optionalChargeType(Long chargeTypeId) {
		if (chargeTypeId == null) {
			return null;
		}
		return chargeTypes.findById(chargeTypeId)
				.orElseThrow(() -> new AssetApplyException("Charge type not found: " + chargeTypeId));
	}

	private MarketDataFeedTypeHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Market data feed type update/terminate requires assetIdentityId and baseHistoryId");
		}
		MarketDataFeedTypeHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Market data feed type history not found: " + command.baseHistoryId()));
		if (!prior.getMarketDataFeedTypeId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to market data feed type " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale market data feed type baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(MarketDataFeedTypeHistory prior, MarketDataFeedTypeHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(
				prior.getMarketDataFeedTypeHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(
				created.getMarketDataFeedTypeHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getMarketDataFeedTypeId(), List.copyOf(links));
	}
}
