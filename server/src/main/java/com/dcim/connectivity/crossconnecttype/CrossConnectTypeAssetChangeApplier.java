package com.dcim.connectivity.crossconnecttype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.connectivity.chargetype.ChargeTypeIdentity;
import com.dcim.connectivity.chargetype.ChargeTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CrossConnectTypeAssetChangeApplier
		extends AbstractAssetChangeApplier<CrossConnectTypeIdentity, CrossConnectTypeHistory> {

	private final ChargeTypeIdentityRepository chargeTypes;

	CrossConnectTypeAssetChangeApplier(
			CrossConnectTypeIdentityRepository identities,
			CrossConnectTypeHistoryRepository history,
			ChargeTypeIdentityRepository chargeTypes,
			JsonPayloads payloads) {
		super(
				"CROSS_CONNECT_TYPE",
				"cross connect type",
				identities,
				history,
				payloads,
				CrossConnectTypeIdentity::new,
				CrossConnectTypeIdentity::getCrossConnectTypeId,
				CrossConnectTypeHistory::getCrossConnectTypeId,
				CrossConnectTypeHistory::getCrossConnectTypeHistoryId);
		this.chargeTypes = chargeTypes;
	}

	@Override
	protected CrossConnectTypeHistory createAdd(
			CrossConnectTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new CrossConnectTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "crossConnectTypeName"),
				optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId")),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CrossConnectTypeHistory createUpdate(
			CrossConnectTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		ChargeTypeIdentity chargeType = body.has("chargeTypeId")
				? optionalChargeType(JsonPayloads.longOrNull(body, "chargeTypeId"))
				: prior.getChargeTypeIdentity();
		return new CrossConnectTypeHistory(
				prior.getCrossConnectTypeIdentity(),
				JsonPayloads.requiredText(body, "crossConnectTypeName"),
				chargeType,
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected CrossConnectTypeHistory createTerminate(CrossConnectTypeHistory prior, AssetApplyCommand command) {
		return new CrossConnectTypeHistory(
				prior.getCrossConnectTypeIdentity(),
				prior.getCrossConnectTypeName(),
				prior.getChargeTypeIdentity(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private ChargeTypeIdentity optionalChargeType(Long chargeTypeId) {
		if (chargeTypeId == null) {
			return null;
		}
		return chargeTypes.findById(chargeTypeId)
				.orElseThrow(() -> new AssetApplyException("Charge type not found: " + chargeTypeId));
	}
}
