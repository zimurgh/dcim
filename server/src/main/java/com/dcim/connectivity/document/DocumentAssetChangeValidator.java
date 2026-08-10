package com.dcim.connectivity.document;

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
class DocumentAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ACTIONS = Set.of("ADD", "UPDATE", "TERMINATE");
	private static final Set<String> ALLOWED_FIELDS = Set.of("documentName", "crossConnectId");

	private final DocumentHistoryRepository history;
	private final CrossConnectService crossConnects;
	private final JsonPayloads payloads;

	DocumentAssetChangeValidator(
			DocumentHistoryRepository history,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		this.history = history;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "DOCUMENT".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		if (!ACTIONS.contains(command.action())) {
			issues.add(new ValidationIssue(
					ValidationCodes.UNSUPPORTED_ACTION, "Unsupported document action: " + command.action()));
			return issues;
		}
		JsonNode body = readBody(command, issues);
		if (body == null) {
			return issues;
		}
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));

		boolean isAdd = "ADD".equals(command.action());
		boolean isTerminate = "TERMINATE".equals(command.action());

		DocumentHistory base = null;
		if (!isAdd) {
			base = PayloadValidation.validateConcurrency(
					command.assetIdentityId(),
					command.baseHistoryId(),
					history::findById,
					DocumentHistory::getDocumentId,
					AuditHistory::isCurrent,
					issues);
		}

		if (isTerminate) {
			return issues;
		}

		PayloadValidation.requireText(body, "documentName", issues);
		String name = PayloadValidation.textOrNull(body, "documentName");

		Long priorCrossConnectId = base == null ? null : base.getCrossConnectId();
		Long crossConnectId = PayloadValidation.resolveRequiredReference(
				body, "crossConnectId", isAdd, priorCrossConnectId, issues,
				id -> crossConnects.findCurrent(id).map(CrossConnectDto::status).orElse(null));

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
				.filter(h -> h.getDocumentName().equalsIgnoreCase(name))
				.map(DocumentHistory::getDocumentId)
				.filter(id -> !Objects.equals(id, selfId))
				.toList();
		if (!clashes.isEmpty()) {
			issues.add(new ValidationIssue(
					ValidationCodes.NAME_CLASH,
					"documentName",
					"documentName already used by an active document on this cross connect",
					clashes));
		}
	}
}
