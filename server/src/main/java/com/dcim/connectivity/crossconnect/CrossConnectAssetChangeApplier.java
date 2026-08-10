package com.dcim.connectivity.crossconnect;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeIdentity;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeIdentityRepository;
import com.dcim.connectivity.latency.LatencyIdentity;
import com.dcim.connectivity.latency.LatencyIdentityRepository;
import com.dcim.connectivity.speed.SpeedIdentity;
import com.dcim.connectivity.speed.SpeedIdentityRepository;
import com.dcim.organization.firm.FirmIdentity;
import com.dcim.organization.firm.FirmIdentityRepository;
import com.dcim.organization.marketsegment.MarketSegmentIdentity;
import com.dcim.organization.marketsegment.MarketSegmentIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectAssetChangeApplier implements AssetChangeApplier {

	private final CrossConnectIdentityRepository identities;
	private final CrossConnectHistoryRepository history;
	private final CrossConnectTypeIdentityRepository crossConnectTypes;
	private final LatencyIdentityRepository latencies;
	private final SpeedIdentityRepository speeds;
	private final MarketSegmentIdentityRepository marketSegments;
	private final FirmIdentityRepository firms;
	private final JsonPayloads payloads;

	CrossConnectAssetChangeApplier(
			CrossConnectIdentityRepository identities,
			CrossConnectHistoryRepository history,
			CrossConnectTypeIdentityRepository crossConnectTypes,
			LatencyIdentityRepository latencies,
			SpeedIdentityRepository speeds,
			MarketSegmentIdentityRepository marketSegments,
			FirmIdentityRepository firms,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.crossConnectTypes = crossConnectTypes;
		this.latencies = latencies;
		this.speeds = speeds;
		this.marketSegments = marketSegments;
		this.firms = firms;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CROSS_CONNECT".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported cross connect action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "crossConnectName");
		String circuitId = JsonPayloads.requiredText(body, "circuitId");
		CrossConnectTypeIdentity type = requireType(JsonPayloads.requiredLong(body, "crossConnectTypeId"));
		LatencyIdentity latency = requireLatency(JsonPayloads.requiredLong(body, "latencyId"));
		SpeedIdentity speed = requireSpeed(JsonPayloads.requiredLong(body, "speedId"));
		MarketSegmentIdentity marketSegment = optionalMarketSegment(
				JsonPayloads.longOrNull(body, "marketSegmentId"));
		FirmIdentity owner = requireFirm(JsonPayloads.requiredLong(body, "ownerFirmId"), "owner");
		FirmIdentity billing = requireFirm(JsonPayloads.requiredLong(body, "billingFirmId"), "billing");
		FirmIdentity provider = optionalFirm(JsonPayloads.longOrNull(body, "providerFirmId"), "provider");
		CrossConnectIdentity identity = identities.saveAndFlush(new CrossConnectIdentity());
		CrossConnectHistory created = history.saveAndFlush(new CrossConnectHistory(
				identity,
				name,
				circuitId,
				type,
				latency,
				speed,
				marketSegment,
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
				identity.getCrossConnectId(),
				List.of(new AssetHistoryLink(created.getCrossConnectHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		CrossConnectHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "crossConnectName");
		String circuitId = body.hasNonNull("circuitId")
				? JsonPayloads.requiredText(body, "circuitId")
				: prior.getCircuitId();
		CrossConnectTypeIdentity type = body.hasNonNull("crossConnectTypeId")
				? requireType(JsonPayloads.requiredLong(body, "crossConnectTypeId"))
				: prior.getCrossConnectTypeIdentity();
		LatencyIdentity latency = body.hasNonNull("latencyId")
				? requireLatency(JsonPayloads.requiredLong(body, "latencyId"))
				: prior.getLatencyIdentity();
		SpeedIdentity speed = body.hasNonNull("speedId")
				? requireSpeed(JsonPayloads.requiredLong(body, "speedId"))
				: prior.getSpeedIdentity();
		MarketSegmentIdentity marketSegment = body.has("marketSegmentId")
				? optionalMarketSegment(JsonPayloads.longOrNull(body, "marketSegmentId"))
				: prior.getMarketSegmentIdentity();
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
		CrossConnectHistory created = history.saveAndFlush(new CrossConnectHistory(
				prior.getCrossConnectIdentity(),
				name,
				circuitId,
				type,
				latency,
				speed,
				marketSegment,
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
		CrossConnectHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		CrossConnectHistory created = history.saveAndFlush(new CrossConnectHistory(
				prior.getCrossConnectIdentity(),
				prior.getCrossConnectName(),
				prior.getCircuitId(),
				prior.getCrossConnectTypeIdentity(),
				prior.getLatencyIdentity(),
				prior.getSpeedIdentity(),
				prior.getMarketSegmentIdentity(),
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

	private CrossConnectTypeIdentity requireType(Long crossConnectTypeId) {
		return crossConnectTypes.findById(crossConnectTypeId)
				.orElseThrow(() -> new AssetApplyException("Cross connect type not found: " + crossConnectTypeId));
	}

	private LatencyIdentity requireLatency(Long latencyId) {
		return latencies.findById(latencyId)
				.orElseThrow(() -> new AssetApplyException("Latency not found: " + latencyId));
	}

	private SpeedIdentity requireSpeed(Long speedId) {
		return speeds.findById(speedId)
				.orElseThrow(() -> new AssetApplyException("Speed not found: " + speedId));
	}

	private MarketSegmentIdentity optionalMarketSegment(Long marketSegmentId) {
		if (marketSegmentId == null) {
			return null;
		}
		return marketSegments.findById(marketSegmentId)
				.orElseThrow(() -> new AssetApplyException("Market segment not found: " + marketSegmentId));
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

	private CrossConnectHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException(
					"Cross connect update/terminate requires assetIdentityId and baseHistoryId");
		}
		CrossConnectHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException(
						"Cross connect history not found: " + command.baseHistoryId()));
		if (!prior.getCrossConnectId().equals(command.assetIdentityId())) {
			throw new AssetApplyException(
					"baseHistoryId does not belong to cross connect " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale cross connect baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(CrossConnectHistory prior, CrossConnectHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getCrossConnectHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getCrossConnectHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getCrossConnectId(), List.copyOf(links));
	}
}
