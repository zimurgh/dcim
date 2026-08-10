package com.dcim.connectivity.document;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.crossconnect.CrossConnectIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class DocumentAssetChangeApplier extends AbstractAssetChangeApplier<DocumentIdentity, DocumentHistory> {

	private final CrossConnectIdentityRepository crossConnects;

	DocumentAssetChangeApplier(
			DocumentIdentityRepository identities,
			DocumentHistoryRepository history,
			CrossConnectIdentityRepository crossConnects,
			JsonPayloads payloads) {
		super(
				"DOCUMENT",
				"document",
				identities,
				history,
				payloads,
				DocumentIdentity::new,
				DocumentIdentity::getDocumentId,
				DocumentHistory::getDocumentId,
				DocumentHistory::getDocumentHistoryId);
		this.crossConnects = crossConnects;
	}

	@Override
	protected DocumentHistory createAdd(DocumentIdentity identity, JsonNode body, AssetApplyCommand command) {
		CrossConnectIdentity crossConnect = requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"));
		return new DocumentHistory(
				identity,
				crossConnect,
				JsonPayloads.requiredText(body, "documentName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected DocumentHistory createUpdate(DocumentHistory prior, JsonNode body, AssetApplyCommand command) {
		CrossConnectIdentity crossConnect = body.hasNonNull("crossConnectId")
				? requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"))
				: prior.getCrossConnectIdentity();
		return new DocumentHistory(
				prior.getDocumentIdentity(),
				crossConnect,
				JsonPayloads.requiredText(body, "documentName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected DocumentHistory createTerminate(DocumentHistory prior, AssetApplyCommand command) {
		return new DocumentHistory(
				prior.getDocumentIdentity(),
				prior.getCrossConnectIdentity(),
				prior.getDocumentName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private CrossConnectIdentity requireCrossConnect(Long crossConnectId) {
		return crossConnects.findById(crossConnectId)
				.orElseThrow(() -> new AssetApplyException("Cross connect not found: " + crossConnectId));
	}
}
