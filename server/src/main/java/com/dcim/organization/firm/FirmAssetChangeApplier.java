package com.dcim.organization.firm;

import java.util.ArrayList;
import java.util.List;

import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.AssetApplyResult;
import com.dcim.asset.AssetChangeApplier;
import com.dcim.asset.AssetHistoryLink;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class FirmAssetChangeApplier implements AssetChangeApplier {

	private final FirmIdentityRepository identities;
	private final FirmHistoryRepository history;
	private final JsonPayloads payloads;

	FirmAssetChangeApplier(
			FirmIdentityRepository identities,
			FirmHistoryRepository history,
			JsonPayloads payloads) {
		this.identities = identities;
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "FIRM".equals(assetType);
	}

	@Override
	public AssetApplyResult apply(AssetApplyCommand command) {
		return switch (command.action()) {
			case "ADD" -> add(command);
			case "UPDATE" -> update(command);
			case "TERMINATE" -> terminate(command);
			default -> throw new AssetApplyException("Unsupported firm action: " + command.action());
		};
	}

	private AssetApplyResult add(AssetApplyCommand command) {
		JsonNode body = payloads.read(command.payloadJson());
		String firmName = JsonPayloads.requiredText(body, "firmName");
		String parentFirmName = JsonPayloads.textOrNull(body, "parentFirmName");
		FirmIdentity identity = identities.saveAndFlush(new FirmIdentity());
		FirmHistory created = history.saveAndFlush(new FirmHistory(
				identity,
				firmName,
				parentFirmName,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return new AssetApplyResult(
				identity.getFirmId(),
				List.of(new AssetHistoryLink(created.getFirmHistoryId(), AssetHistoryLink.ROLE_CREATED)));
	}

	private AssetApplyResult update(AssetApplyCommand command) {
		FirmHistory prior = requireCurrentBase(command);
		JsonNode body = payloads.read(command.payloadJson());
		String firmName = JsonPayloads.requiredText(body, "firmName");
		String parentFirmName = JsonPayloads.textOrNull(body, "parentFirmName");
		prior.close(command.validOn());
		FirmHistory created = history.saveAndFlush(new FirmHistory(
				prior.getFirmIdentity(),
				firmName,
				parentFirmName,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private AssetApplyResult terminate(AssetApplyCommand command) {
		FirmHistory prior = requireCurrentBase(command);
		prior.close(command.validOn());
		FirmHistory created = history.saveAndFlush(new FirmHistory(
				prior.getFirmIdentity(),
				prior.getFirmName(),
				prior.getParentFirmName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus()));
		return result(prior, created);
	}

	private FirmHistory requireCurrentBase(AssetApplyCommand command) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			throw new AssetApplyException("Firm update/terminate requires assetIdentityId and baseHistoryId");
		}
		FirmHistory prior = history.findById(command.baseHistoryId())
				.orElseThrow(() -> new AssetApplyException("Firm history not found: " + command.baseHistoryId()));
		if (!prior.getFirmId().equals(command.assetIdentityId())) {
			throw new AssetApplyException("baseHistoryId does not belong to firm " + command.assetIdentityId());
		}
		if (!prior.isCurrent()) {
			throw new AssetApplyException("Stale firm baseHistoryId (already closed): " + command.baseHistoryId());
		}
		return prior;
	}

	private static AssetApplyResult result(FirmHistory prior, FirmHistory created) {
		List<AssetHistoryLink> links = new ArrayList<>();
		links.add(new AssetHistoryLink(prior.getFirmHistoryId(), AssetHistoryLink.ROLE_CLOSED_PRIOR));
		links.add(new AssetHistoryLink(created.getFirmHistoryId(), AssetHistoryLink.ROLE_CREATED));
		return new AssetApplyResult(prior.getFirmId(), List.copyOf(links));
	}
}
