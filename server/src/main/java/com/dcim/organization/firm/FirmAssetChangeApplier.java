package com.dcim.organization.firm;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class FirmAssetChangeApplier extends AbstractAssetChangeApplier<FirmIdentity, FirmHistory> {

	FirmAssetChangeApplier(
			FirmIdentityRepository identities,
			FirmHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"FIRM",
				"firm",
				identities,
				history,
				payloads,
				FirmIdentity::new,
				FirmIdentity::getFirmId,
				FirmHistory::getFirmId,
				FirmHistory::getFirmHistoryId);
	}

	@Override
	protected FirmHistory createAdd(FirmIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new FirmHistory(
				identity,
				JsonPayloads.requiredText(body, "firmName"),
				JsonPayloads.textOrNull(body, "parentFirmName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected FirmHistory createUpdate(FirmHistory prior, JsonNode body, AssetApplyCommand command) {
		return new FirmHistory(
				prior.getFirmIdentity(),
				JsonPayloads.requiredText(body, "firmName"),
				JsonPayloads.textOrNull(body, "parentFirmName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected FirmHistory createTerminate(FirmHistory prior, AssetApplyCommand command) {
		return new FirmHistory(
				prior.getFirmIdentity(),
				prior.getFirmName(),
				prior.getParentFirmName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}
}
