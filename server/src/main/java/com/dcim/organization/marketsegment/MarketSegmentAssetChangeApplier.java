package com.dcim.organization.marketsegment;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketSegmentAssetChangeApplier implements AssetChangeApplier {

	private final MarketSegmentIdentityRepository identities;
	private final MarketSegmentHistoryRepository history;
	private final JsonPayloads payloads;

	MarketSegmentAssetChangeApplier(
			MarketSegmentIdentityRepository identities,
			MarketSegmentHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "MARKET_SEGMENT".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported market segment action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketSegmentName");
		MarketSegmentType type = requireMarketSegmentType(body);
		MarketSegmentIdentity identity = identities.saveAndFlush(new MarketSegmentIdentity());
		MarketSegmentHistory created = history.saveAndFlush(new MarketSegmentHistory(
				identity,
				name,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getMarketSegmentId(),
				List.of(new AssetHistoryLink(created.getMarketSegmentHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		MarketSegmentHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "marketSegmentName");
		MarketSegmentType type = requireMarketSegmentType(body);
		prior.close(command.validOn());
		MarketSegmentHistory created = history.saveAndFlush(new MarketSegmentHistory(
				prior.getMarketSegmentIdentity(),
				name,
				type,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		MarketSegmentHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		MarketSegmentHistory created = history.saveAndFlush(new MarketSegmentHistory(
				prior.getMarketSegmentIdentity(),
				prior.getMarketSegmentName(),
				prior.getMarketSegmentType(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
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

	private MarketSegmentHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Market segment update/terminate requires assetIdentityId and baseHistoryId");
		}
		MarketSegmentHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Market segment history not found: " + command.baseHistoryId()));
		if (!prior.getMarketSegmentId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to market segment " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale market segment baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(MarketSegmentHistory prior, MarketSegmentHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getMarketSegmentHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getMarketSegmentHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getMarketSegmentId(), List.copyOf(links));
	}
}
