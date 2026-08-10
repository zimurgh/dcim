package com.dcim.connectivity.document;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationIssue;
import com.dcim.connectivity.crossconnect.CrossConnectDto;
import com.dcim.connectivity.crossconnect.CrossConnectService;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class DocumentAssetChangeValidator extends AbstractAssetChangeValidator<DocumentHistory> {

	private final DocumentHistoryRepository history;
	private final CrossConnectService crossConnects;

	DocumentAssetChangeValidator(
			DocumentHistoryRepository history,
			CrossConnectService crossConnects,
			JsonPayloads payloads) {
		super("DOCUMENT", "document", Set.of("documentName", "crossConnectId"), history,
				DocumentHistory::getDocumentId, payloads);
		this.history = history;
		this.crossConnects = crossConnects;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, DocumentHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "documentName", issues);
		String name = PayloadValidation.textOrNull(body, "documentName");

		boolean isAdd = prior == null;
		Long priorCrossConnectId = prior == null ? null : prior.getCrossConnectId();
		Long crossConnectId = PayloadValidation.resolveRequiredReference(
				body, "crossConnectId", isAdd, priorCrossConnectId, issues,
				id -> crossConnects.findCurrent(id).map(CrossConnectDto::status).orElse(null));

		if (name != null && crossConnectId != null) {
			validateNameClash(name, crossConnectId, command.assetIdentityId(), issues);
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
