package com.dcim.connectivity.speed;

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

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class SpeedAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("speedName", "speedType");

	private final SpeedHistoryRepository history;
	private final CrossConnectService crossConnects;
	private final JsonPayloads payloads;

	SpeedAssetChangeValidator(
			SpeedHistoryRepository history,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		this.history = history;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "SPEED".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION, "Unsupported speed action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		SpeedHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					SpeedHistory::getSpeedId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			if (base != null) {
				validateTerminateGuard(base, context, issues);
			}
			return issues;
		}

		PayloadValidation.requireText(body, "speedName", issues);
		String name = PayloadValidation.textOrNull(body, "speedName");
		SpeedType type = parseSpeedType(body, issues);

		Long selfId = command.assetIdentityId();
		if (type != null) {
			validateTypeClash(type, selfId, issues);
		}
		if (name != null) {
			validateNameClash(name, selfId, issues);
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

	private static SpeedType parseSpeedType(JsonNode body, List<ValidationIssue> issues) {
		String raw = PayloadValidation.textOrNull(body, "speedType");
		if (raw == null) {
			issues.add(new ValidationIssue(ValidationCodes.MISSING_FIELD, "speedType", "Payload missing speedType"));
			return null;
		}
		try {
			return SpeedType.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			issues.add(new ValidationIssue(
					ValidationCodes.INVALID_VALUE, "speedType", "speedType must be 1G or 10G: " + raw));
			return null;
		}
	}

	private void validateTypeClash(SpeedType type, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getSpeedType() == type)
				.map(SpeedHistory::getSpeedId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.VALUE_CLASH,
					"speedType",
					"speedType " + type + " already used by an active speed",
					clashes));
		}
	}

	private void validateNameClash(String name, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getSpeedName().equalsIgnoreCase(name))
				.map(SpeedHistory::getSpeedId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH, "speedName", "speedName already used by an active speed", clashes));
		}
	}

	private void validateTerminateGuard(SpeedHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blocking = crossConnects.listCurrentBySpeedId(base.getSpeedId()).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectDto::crossConnectId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (!blocking.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Speed is referenced by active cross connects",
					blocking));
		}
	}
}
