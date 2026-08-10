package com.dcim.site.rackdevicetype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceTypeAssetChangeApplier
		extends AbstractAssetChangeApplier<RackDeviceTypeIdentity, RackDeviceTypeHistory> {

	RackDeviceTypeAssetChangeApplier(
			RackDeviceTypeIdentityRepository identities,
			RackDeviceTypeHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE_TYPE",
				"rack device type",
				identities,
				history,
				payloads,
				RackDeviceTypeIdentity::new,
				RackDeviceTypeIdentity::getRackDeviceTypeId,
				RackDeviceTypeHistory::getRackDeviceTypeId,
				RackDeviceTypeHistory::getRackDeviceTypeHistoryId);
	}

	@Override
	protected RackDeviceTypeHistory createAdd(
			RackDeviceTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new RackDeviceTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "rackDeviceTypeName"),
				requireKind(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDeviceTypeHistory createUpdate(
			RackDeviceTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		return new RackDeviceTypeHistory(
				prior.getRackDeviceTypeIdentity(),
				JsonPayloads.requiredText(body, "rackDeviceTypeName"),
				requireKind(body),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDeviceTypeHistory createTerminate(RackDeviceTypeHistory prior, AssetApplyCommand command) {
		return new RackDeviceTypeHistory(
				prior.getRackDeviceTypeIdentity(),
				prior.getRackDeviceTypeName(),
				prior.getRackDeviceTypeKind(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private static RackDeviceTypeKind requireKind(JsonNode body) {
		String raw = JsonPayloads.requiredText(body, "rackDeviceTypeKind");
		try {
			return RackDeviceTypeKind.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			throw new AssetApplyException(
					"rackDeviceTypeKind must be Patch Panel, Extranet Switch, Matrix Switch, or Tap: " + raw);
		}
	}
}
