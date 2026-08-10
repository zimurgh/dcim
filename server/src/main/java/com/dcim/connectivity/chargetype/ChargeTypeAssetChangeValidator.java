package com.dcim.connectivity.chargetype;

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
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeDto;
import com.dcim.connectivity.crossconnecttype.CrossConnectTypeService;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeDto;
import com.dcim.connectivity.marketdatafeedtype.MarketDataFeedTypeService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ChargeTypeAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("chargeTypeName");

	private final ChargeTypeHistoryRepository history;
	private final CrossConnectTypeService crossConnectTypes;
	private final MarketDataFeedTypeService marketDataFeedTypes;
	private final JsonPayloads payloads;

	ChargeTypeAssetChangeValidator(
			ChargeTypeHistoryRepository history,
			CrossConnectTypeService crossConnectTypes,
			MarketDataFeedTypeService marketDataFeedTypes,
			JsonPayloads payloads) {
		this.history = history;
		this.crossConnectTypes = crossConnectTypes;
		this.marketDataFeedTypes = marketDataFeedTypes;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CHARGE_TYPE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION, "Unsupported charge type action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		ChargeTypeHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					ChargeTypeHistory::getChargeTypeId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			if (base != null) {
				validateTerminateGuard(base, context, issues);
			}
			return issues;
		}

		PayloadValidation.requireText(body, "chargeTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "chargeTypeName");
		if (name != null) {
			validateNameClash(name, command.assetIdentityId(), issues);
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

	private void validateNameClash(String name, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getChargeTypeName().equalsIgnoreCase(name))
				.map(ChargeTypeHistory::getChargeTypeId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"chargeTypeName",
					"chargeTypeName already used by an active charge type",
					clashes));
		}
	}

	private void validateTerminateGuard(ChargeTypeHistory base, ValidationContext context, List<ValidationIssue> issues) {
		Long chargeTypeId = base.getChargeTypeId();
		List<Long> blockingTypes = crossConnectTypes.listCurrentByChargeTypeId(chargeTypeId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectTypeDto::crossConnectTypeId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT_TYPE", id))
				.toList();
		if (!blockingTypes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Charge type is referenced by active cross connect types",
					blockingTypes));
		}
		List<Long> blockingFeedTypes = marketDataFeedTypes.listCurrentByChargeTypeId(chargeTypeId).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(MarketDataFeedTypeDto::marketDataFeedTypeId)
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED_TYPE", id))
				.toList();
		if (!blockingFeedTypes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Charge type is referenced by active market data feed types",
					blockingFeedTypes));
		}
	}
}
