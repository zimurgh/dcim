package com.dcim.connectivity.chargetype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class ChargeTypeAssetChangeApplier extends AbstractAssetChangeApplier<ChargeTypeIdentity, ChargeTypeHistory> {

	ChargeTypeAssetChangeApplier(
			ChargeTypeIdentityRepository identities,
			ChargeTypeHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"CHARGE_TYPE",
				"charge type",
				identities,
				history,
				payloads,
				ChargeTypeIdentity::new,
				ChargeTypeIdentity::getChargeTypeId,
				ChargeTypeHistory::getChargeTypeId,
				ChargeTypeHistory::getChargeTypeHistoryId);
	}

	@Override
	protected ChargeTypeHistory createAdd(
			ChargeTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new ChargeTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "chargeTypeName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected ChargeTypeHistory createUpdate(
			ChargeTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		return new ChargeTypeHistory(
				prior.getChargeTypeIdentity(),
				JsonPayloads.requiredText(body, "chargeTypeName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected ChargeTypeHistory createTerminate(ChargeTypeHistory prior, AssetApplyCommand command) {
		return new ChargeTypeHistory(
				prior.getChargeTypeIdentity(),
				prior.getChargeTypeName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}
}
