package com.dcim.connectivity.document;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.crossconnect.CrossConnectIdentity;
import com.dcim.connectivity.crossconnect.CrossConnectIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class DocumentAssetChangeApplier implements AssetChangeApplier {

	private final DocumentIdentityRepository identities;
	private final DocumentHistoryRepository history;
	private final CrossConnectIdentityRepository crossConnects;
	private final JsonPayloads payloads;

	DocumentAssetChangeApplier(
			DocumentIdentityRepository identities,
			DocumentHistoryRepository history,
			CrossConnectIdentityRepository crossConnects,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.crossConnects = crossConnects;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "DOCUMENT".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported document action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "documentName");
		CrossConnectIdentity crossConnect = requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"));
		DocumentIdentity identity = identities.saveAndFlush(new DocumentIdentity());
		DocumentHistory created = history.saveAndFlush(new DocumentHistory(
				identity,
				crossConnect,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getDocumentId(),
				List.of(new AssetHistoryLink(created.getDocumentHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		DocumentHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String name = JsonPayloads.requiredText(body, "documentName");
		CrossConnectIdentity crossConnect = body.hasNonNull("crossConnectId")
				? requireCrossConnect(JsonPayloads.requiredLong(body, "crossConnectId"))
				: prior.getCrossConnectIdentity();
		prior.close(command.validOn());
		DocumentHistory created = history.saveAndFlush(new DocumentHistory(
				prior.getDocumentIdentity(),
				crossConnect,
				name,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		DocumentHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		DocumentHistory created = history.saveAndFlush(new DocumentHistory(
				prior.getDocumentIdentity(),
				prior.getCrossConnectIdentity(),
				prior.getDocumentName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private CrossConnectIdentity requireCrossConnect(Long crossConnectId) {
		return crossConnects.findById(crossConnectId)
				.orElseThrow(() -> new AssetApplyException("Cross connect not found: " + crossConnectId));
	}

	private DocumentHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Document update/terminate requires assetIdentityId and baseHistoryId");
		}
		DocumentHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Document history not found: " + command.baseHistoryId()));
		if (!prior.getDocumentId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to document " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale document baseHistoryId: " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(DocumentHistory prior, DocumentHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getDocumentHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getDocumentHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getDocumentId(), List.copyOf(links));
	}
}
