package com.dcim.connectivity.crossconnecttype;

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
import com.dcim.connectivity.chargetype.ChargeTypeDto;
import com.dcim.connectivity.chargetype.ChargeTypeService;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectTypeAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("crossConnectTypeName", "chargeTypeId");

	private final CrossConnectTypeHistoryRepository history;
	private final ChargeTypeService chargeTypes;
	private final CrossConnectService crossConnects;
	private final JsonPayloads payloads;

	CrossConnectTypeAssetChangeValidator(
			CrossConnectTypeHistoryRepository history,
			ChargeTypeService chargeTypes,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		this.history = history;
		this.chargeTypes = chargeTypes;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CROSS_CONNECT_TYPE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION,
					"Unsupported cross connect type action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		CrossConnectTypeHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					CrossConnectTypeHistory::getCrossConnectTypeId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			if (base != null) {
				validateTerminateGuard(base, context, issues);
			}
			return issues;
		}

		PayloadValidation.requireText(body, "crossConnectTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "crossConnectTypeName");
		if (name != null) {
			validateNameClash(name, command.assetIdentityId(), issues);
		}

		if (body.has("chargeTypeId")) {
			Long chargeTypeId = PayloadValidation.longOrNull(body, "chargeTypeId", issues);
			if (chargeTypeId != null) {
				String status = chargeTypes.findCurrent(chargeTypeId).map(ChargeTypeDto::status).orElse(null);
				PayloadValidation.requireActiveReference("chargeTypeId", chargeTypeId, status, issues);
			}
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
				.filter(h -> h.getCrossConnectTypeName().equalsIgnoreCase(name))
				.map(CrossConnectTypeHistory::getCrossConnectTypeId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"crossConnectTypeName",
					"crossConnectTypeName already used by an active cross connect type",
					clashes));
		}
	}

	private void validateTerminateGuard(
			CrossConnectTypeHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blocking = crossConnects.listCurrentByCrossConnectTypeId(base.getCrossConnectTypeId()).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectDto::crossConnectId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (!blocking.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Cross connect type is referenced by active cross connects",
					blocking));
		}
	}
}
