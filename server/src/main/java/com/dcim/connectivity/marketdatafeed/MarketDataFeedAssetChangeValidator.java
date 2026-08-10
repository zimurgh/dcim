package com.dcim.connectivity.marketdatafeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.dcim.asset.AssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.AuditHistory;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeDto;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeService;
import com.dcim.organization.firm.FirmDto;
import com.dcim.organization.firm.FirmService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class MarketDataFeedAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of(
			"marketDataFeedName",
			"crossConnectId",
			"marketDataFeedTypeId",
			"ownerFirmId",
			"billingFirmId",
			"providerFirmId");

	private final MarketDataFeedHistoryRepository history;
	private final CrossConnectService crossConnects;
	private final MarketDataFeedTypeService feedTypes;
	private final FirmService firms;
	private final JsonPayloads payloads;

	MarketDataFeedAssetChangeValidator(
			MarketDataFeedHistoryRepository history,
			CrossConnectService crossConnects,
			MarketDataFeedTypeService feedTypes,
			FirmService firms,
			JsonPayloads payloads) {
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
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION,
					"Unsupported market data feed action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		MarketDataFeedHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					MarketDataFeedHistory::getMarketDataFeedId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			return issues;
		}

		PayloadValidation.requireText(body, "marketDataFeedName", issues);
		String name = PayloadValidation.textOrNull(body, "marketDataFeedName");

		Long priorCrossConnectId = base == null ? null : base.getCrossConnectId();
		Long priorFeedTypeId = base == null ? null : base.getMarketDataFeedTypeId();
		Long priorOwnerFirmId = base == null ? null : base.getOwnerFirmId();
		Long priorBillingFirmId = base == null ? null : base.getBillingFirmId();

		Long crossConnectId = PayloadValidation.resolveRequiredReference(
				body, "crossConnectId", isAdd, priorCrossConnectId, issues,
				id -> crossConnects.findCurrent(id).map(CrossConnectDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "marketDataFeedTypeId", isAdd, priorFeedTypeId, issues,
				id -> feedTypes.findCurrent(id).map(MarketDataFeedTypeDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "ownerFirmId", isAdd, priorOwnerFirmId, issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));
		PayloadValidation.resolveRequiredReference(
				body, "billingFirmId", isAdd, priorBillingFirmId, issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));
		PayloadValidation.validateOptionalReference(
				body, "providerFirmId", issues,
				id -> firms.findCurrent(id).map(FirmDto::status).orElse(null));

		if (name != null && crossConnectId != null) {
			validateNameClash(name, crossConnectId, command.assetIdentityId(), issues);
		}

		return issues;
	}

	private JsonNode readBody(AssetValidateCommand command, List<ValidationIssue> issues) {
		try {
			return payloads.read(command.payloadJson());
		}
		catch (RuntimeException ex) {
			issues.add(new ValidationIssue(ValidationCodes.INVALID_PAYLOAD, ex.getMessage()));
			return null;
		}
	}

	private void validateNameClash(String name, Long crossConnectId, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrentByCrossConnectId(crossConnectId).stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getMarketDataFeedName().equalsIgnoreCase(name))
				.map(MarketDataFeedHistory::getMarketDataFeedId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"marketDataFeedName",
					"marketDataFeedName already used by an active feed on this cross connect",
					clashes));
		}
	}
}
