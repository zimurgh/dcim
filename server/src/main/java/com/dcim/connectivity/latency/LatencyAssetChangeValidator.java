package com.dcim.connectivity.latency;

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
class LatencyAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("latencyName", "latencyType");

	private final LatencyHistoryRepository history;
	private final CrossConnectService crossConnects;
	private final JsonPayloads payloads;

	LatencyAssetChangeValidator(
			LatencyHistoryRepository history,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		this.history = history;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "LATENCY".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION, "Unsupported latency action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		LatencyHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					LatencyHistory::getLatencyId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			if (base != null) {
				validateTerminateGuard(base, context, issues);
			}
			return issues;
		}

		PayloadValidation.requireText(body, "latencyName", issues);
		String name = PayloadValidation.textOrNull(body, "latencyName");
		LatencyType type = parseLatencyType(body, issues);

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

	private static LatencyType parseLatencyType(JsonNode body, List<ValidationIssue> issues) {
		String raw = PayloadValidation.textOrNull(body, "latencyType");
		if (raw == null) {
			issues.add(new ValidationIssue(ValidationCodes.MISSING_FIELD, "latencyType", "Payload missing latencyType"));
			return null;
		}
		try {
			return LatencyType.valueOf(raw.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			issues.add(new ValidationIssue(
					ValidationCodes.INVALID_VALUE, "latencyType", "latencyType must be LL or ULL: " + raw));
			return null;
		}
	}

	private void validateTypeClash(LatencyType type, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getLatencyType() == type)
				.map(LatencyHistory::getLatencyId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.VALUE_CLASH,
					"latencyType",
					"latencyType " + type + " already used by an active latency",
					clashes));
		}
	}

	private void validateNameClash(String name, Long selfId, List<ValidationIssue> issues) {
		List<Long> clashes = history.findCurrent().stream()
				.filter(h -> PayloadValidation.isActiveStatus(h.getStatus()))
				.filter(h -> h.getLatencyName().equalsIgnoreCase(name))
				.map(LatencyHistory::getLatencyId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH, "latencyName", "latencyName already used by an active latency",
					clashes));
		}
	}

	private void validateTerminateGuard(LatencyHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blocking = crossConnects.listCurrentByLatencyId(base.getLatencyId()).stream()
				.filter(dto -> PayloadValidation.isActiveStatus(dto.status()))
				.map(CrossConnectDto::crossConnectId)
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (!blocking.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.ACTIVE_REFERENCES,
					null,
					"Latency is referenced by active cross connects",
					blocking));
		}
	}
}
