package com.dcim.workflow.assettype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class AssetTypeAssetChangeApplier extends AbstractAssetChangeApplier<AssetTypeIdentity, AssetTypeHistory> {

	AssetTypeAssetChangeApplier(
			AssetTypeIdentityRepository identities,
			AssetTypeHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"ASSET_TYPE",
				"asset type",
				identities,
				history,
				payloads,
				AssetTypeIdentity::new,
				AssetTypeIdentity::getAssetTypeId,
				AssetTypeHistory::getAssetTypeId,
				AssetTypeHistory::getAssetTypeHistoryId);
	}

	@Override
	protected AssetTypeHistory createAdd(
			AssetTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new AssetTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "assetTypeCode").toUpperCase(),
				JsonPayloads.requiredText(body, "assetTypeName"),
				requiredRank(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected AssetTypeHistory createUpdate(
			AssetTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		return new AssetTypeHistory(
				prior.getAssetTypeIdentity(),
				prior.getAssetTypeCode(),
				JsonPayloads.requiredText(body, "assetTypeName"),
				body.path("applyRank").isMissingNode() || body.path("applyRank").isNull()
						? prior.getApplyRank()
						: requiredRank(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected AssetTypeHistory createTerminate(AssetTypeHistory prior, AssetApplyCommand command) {
		return new AssetTypeHistory(
				prior.getAssetTypeIdentity(),
				prior.getAssetTypeCode(),
				prior.getAssetTypeName(),
				prior.getApplyRank(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private static int requiredRank(JsonNode body) {
		JsonNode node = body.get("applyRank");
		if (node == null || node.isNull() || !node.canConvertToLong()) {
			throw new com.dcim.asset.AssetApplyException("Payload missing applyRank");
		}
		return node.asInt();
	}
}
