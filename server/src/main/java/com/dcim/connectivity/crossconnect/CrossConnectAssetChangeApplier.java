package com.dcim.connectivity.crossconnect;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
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
class CrossConnectAssetChangeApplier extends AbstractAssetChangeApplier<CrossConnectIdentity, CrossConnectHistory> {

	private final CrossConnectTypeIdentityRepository crossConnectTypes;
	private final LatencyIdentityRepository latencies;
	private final SpeedIdentityRepository speeds;
	private final MarketSegmentIdentityRepository marketSegments;
	private final FirmIdentityRepository firms;

	CrossConnectAssetChangeApplier(
			CrossConnectIdentityRepository identities,
			CrossConnectHistoryRepository history,
			CrossConnectTypeIdentityRepository crossConnectTypes,
			LatencyIdentityRepository latencies,
			SpeedIdentityRepository speeds,
			MarketSegmentIdentityRepository marketSegments,
			FirmIdentityRepository firms,
			JsonPayloads payloads) {
		super(
				"CROSS_CONNECT",
				"cross connect",
				identities,
				history,
				payloads,
				CrossConnectIdentity::new,
				CrossConnectIdentity::getCrossConnectId,
				CrossConnectHistory::getCrossConnectId,
				CrossConnectHistory::getCrossConnectHistoryId);
		this.crossConnectTypes = crossConnectTypes;
		this.latencies = latencies;
		this.speeds = speeds;
		this.marketSegments = marketSegments;
		this.firms = firms;
	}

	@Override
	protected CrossConnectHistory createAdd(CrossConnectIdentity identity, JsonNode body, AssetApplyCommand command) {
		CrossConnectTypeIdentity type = requireType(JsonPayloads.requiredLong(body, "crossConnectTypeId"));
		LatencyIdentity latency = requireLatency(JsonPayloads.requiredLong(body, "latencyId"));
		SpeedIdentity speed = requireSpeed(JsonPayloads.requiredLong(body, "speedId"));
		MarketSegmentIdentity marketSegment = optionalMarketSegment(
				JsonPayloads.longOrNull(body, "marketSegmentId"));
		FirmIdentity owner = requireFirm(JsonPayloads.requiredLong(body, "ownerFirmId"), "owner");
		FirmIdentity billing = requireFirm(JsonPayloads.requiredLong(body, "billingFirmId"), "billing");
		FirmIdentity provider = optionalFirm(JsonPayloads.longOrNull(body, "providerFirmId"), "provider");
		return new CrossConnectHistory(
				identity,
				JsonPayloads.requiredText(body, "crossConnectName"),
				JsonPayloads.requiredText(body, "circuitId"),
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
				command.committedStatus());
	}

	@Override
	protected CrossConnectHistory createUpdate(
			CrossConnectHistory prior, JsonNode body, AssetApplyCommand command) {
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
		return new CrossConnectHistory(
				prior.getCrossConnectIdentity(),
				JsonPayloads.requiredText(body, "crossConnectName"),
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
				command.committedStatus());
	}

	@Override
	protected CrossConnectHistory createTerminate(CrossConnectHistory prior, AssetApplyCommand command) {
		return new CrossConnectHistory(
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
				command.committedStatus());
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
}
