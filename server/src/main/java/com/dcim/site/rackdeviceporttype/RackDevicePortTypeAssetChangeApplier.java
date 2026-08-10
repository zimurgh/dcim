package com.dcim.site.rackdeviceporttype;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.JsonPayloads;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDevicePortTypeAssetChangeApplier
		extends AbstractAssetChangeApplier<RackDevicePortTypeIdentity, RackDevicePortTypeHistory> {

	RackDevicePortTypeAssetChangeApplier(
			RackDevicePortTypeIdentityRepository identities,
			RackDevicePortTypeHistoryRepository history,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE_PORT_TYPE",
				"rack device port type",
				identities,
				history,
				payloads,
				RackDevicePortTypeIdentity::new,
				RackDevicePortTypeIdentity::getRackDevicePortTypeId,
				RackDevicePortTypeHistory::getRackDevicePortTypeId,
				RackDevicePortTypeHistory::getRackDevicePortTypeHistoryId);
	}

	@Override
	protected RackDevicePortTypeHistory createAdd(
			RackDevicePortTypeIdentity identity, JsonNode body, AssetApplyCommand command) {
		return new RackDevicePortTypeHistory(
				identity,
				JsonPayloads.requiredText(body, "rackDevicePortTypeName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDevicePortTypeHistory createUpdate(
			RackDevicePortTypeHistory prior, JsonNode body, AssetApplyCommand command) {
		return new RackDevicePortTypeHistory(
				prior.getRackDevicePortTypeIdentity(),
				JsonPayloads.requiredText(body, "rackDevicePortTypeName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDevicePortTypeHistory createTerminate(
			RackDevicePortTypeHistory prior, AssetApplyCommand command) {
		return new RackDevicePortTypeHistory(
				prior.getRackDevicePortTypeIdentity(),
				prior.getRackDevicePortTypeName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}
}
