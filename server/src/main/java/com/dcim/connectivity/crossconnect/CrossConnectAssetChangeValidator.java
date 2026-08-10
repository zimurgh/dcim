package com.dcim.connectivity.crossconnect;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.cable.CableDto;
import com.dcim.connectivity.cable.CableService;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeDto;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeService;
import com.dcim.connectivity.document.DocumentDto;
import com.dcim.connectivity.document.DocumentService;
import com.dcim.connectivity.latency.LatencyDto;
import com.dcim.connectivity.latency.LatencyService;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedDto;
import com.dcim.connectivity.marketdatafeed.MarketDataFeedService;
import com.dcim.connectivity.speed.SpeedDto;
import com.dcim.connectivity.speed.SpeedService;
import com.dcim.organization.firm.FirmDto;
import com.dcim.organization.firm.FirmService;
import com.dcim.organization.marketsegment.MarketSegmentDto;
import com.dcim.organization.marketsegment.MarketSegmentService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectAssetChangeValidator extends AbstractAssetChangeValidator<CrossConnectHistory> {

	private static final Set<String> ALLOWED_FIELDS = Set.of(
			"crossConnectName",
			"circuitId",
			"crossConnectTypeId",
			"latencyId",
			"speedId",
			"marketSegmentId",
			"ownerFirmId",
			"billingFirmId",
			"providerFirmId");

	private final CrossConnectHistoryRepository history;
	private final CrossConnectTypeService crossConnectTypes;
	private final LatencyService latencies;
	private final SpeedService speeds;
	private final MarketSegmentService marketSegments;
	private final FirmService firms;
	private final MarketDataFeedService marketDataFeeds;
	private final DocumentService documents;
	private final CableService cables;

	CrossConnectAssetChangeValidator(
			CrossConnectHistoryRepository history,
			CrossConnectTypeService crossConnectTypes,
			LatencyService latencies,
			SpeedService speeds,
			MarketSegmentService marketSegments,
			FirmService firms,
			MarketDataFeedService marketDataFeeds,
			DocumentService documents,
			CableService cables,
			JsonPayloads payloads) {
		super("CROSS_CONNECT", "cross connect", ALLOWED_FIELDS, history, CrossConnectHistory::getCrossConnectId,
				payloads);
		this.history = history;
		this.crossConnectTypes = crossConnectTypes;
		this.latencies = latencies;
		this.speeds = speeds;
		this.marketSegments = marketSegments;
		this.firms = firms;
		this.marketDataFeeds = marketDataFeeds;
		this.documents = documents;
		this.cables = cables;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, CrossConnectHistory base, List<ValidationIssue> issues) {
		boolean isAdd = base == null;

		PayloadValidation.requireText(body, "crossConnectName", issues);

		String priorCircuitId = base == null ? null : base.getCircuitId();
		String circuitId = PayloadValidation.resolveRequiredText(body, "circuitId", isAdd, priorCircuitId, issues);

		Long priorCrossConnectTypeId = base == null ? null : base.getCrossConnectTypeId();
		Long priorLatencyId = base == null ? null : base.getLatencyId();
		Long priorSpeedId = base == null ? null : base.getSpeedId();
		Long priorOwnerFirmId = base == null ? null : base.getOwnerFirmId();
		Long priorBillingFirmId = base == null ? null : base.getBillingFirmId();

		PayloadValidation.resolveRequiredReference(
				body, "crossConnectTypeId", isAdd, priorCrossConnectTypeId, issues,
				id -> crossConnectTypes.findCurrent(id).map(CrossConnectTypeDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "latencyId", isAdd, priorLatencyId, issues,
				id -> latencies.findCurrent(id).map(LatencyDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "speedId", isAdd, priorSpeedId, issues,
				id -> speeds.findCurrent(id).map(SpeedDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "ownerFirmId", isAdd, priorOwnerFirmId, issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "billingFirmId", isAdd, priorBillingFirmId, issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));

		PayloadValidation.validateOptionalReference(
				body, "marketSegmentId", issues,
				id -> marketSegments.findCurrent(id).map(MarketSegmentDto::status).orElse(null));
		PayloadValidation.validateOptionalReference(
				body, "providerFirmId", issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));

		if (circuitId != null) {
			validateCircuitClash(circuitId, command.assetIdentityId(), issues);
		}
	}

	@Override
	protected void validateTerminate(
			CrossConnectHistory base, ValidationContext context, List<ValidationIssue> issues) {
		Long crossConnectId = base.getCrossConnectId();

		List<Long> liveFeeds = marketDataFeeds.listCurrentByCrossConnect(crossConnectId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(MarketDataFeedDto::marketDataFeedId)
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED", id))
				.toList();
		if (!liveFeeds.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_CHILDREN, null, "Cross connect has active market data feeds", liveFeeds));
		}

		List<Long> liveDocuments = documents.listCurrentByCrossConnect(crossConnectId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(DocumentDto::documentId)
				.filter(id -> !context.coversTerminate("DOCUMENT", id))
				.toList();
		if (!liveDocuments.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_CHILDREN, null, "Cross connect has active documents", liveDocuments));
		}

		List<Long> liveCables = cables.listCurrentByCrossConnect(crossConnectId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CableDto::cableId)
				.filter(id -> !context.coversTerminate("CABLE", id))
				.toList();
		if (!liveCables.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_CHILDREN, null, "Cross connect has active cables", liveCables));
		}
	}

	private void validateCircuitClash(String circuitId, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrentByCircuitId(circuitId).stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.map(CrossConnectHistory::getCrossConnectId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.VALUE_CLASH,
					"circuitId",
					"circuitId already used by an active cross connect",
					clashes));
		}
	}
}
