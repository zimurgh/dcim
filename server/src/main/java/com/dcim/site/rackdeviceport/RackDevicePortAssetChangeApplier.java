package com.dcim.site.rackdeviceport;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.rackdevice.RackDeviceIdentity;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentity;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDevicePortAssetChangeApplier
		extends AbstractAssetChangeApplier<RackDevicePortIdentity, RackDevicePortHistory> {

	private final RackDeviceIdentityRepository devices;
	private final RackDevicePortTypeIdentityRepository portTypes;

	RackDevicePortAssetChangeApplier(
			RackDevicePortIdentityRepository identities,
			RackDevicePortHistoryRepository history,
			RackDeviceIdentityRepository devices,
			RackDevicePortTypeIdentityRepository portTypes,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE_PORT",
				"rack device port",
				identities,
				history,
				payloads,
				RackDevicePortIdentity::new,
				RackDevicePortIdentity::getRackDevicePortId,
				RackDevicePortHistory::getRackDevicePortId,
				RackDevicePortHistory::getRackDevicePortHistoryId);
		this.devices = devices;
		this.portTypes = portTypes;
	}

	@Override
	protected RackDevicePortHistory createAdd(
			RackDevicePortIdentity identity, JsonNode body, AssetApplyCommand command) {
		RackDeviceIdentity device = requireDevice(JsonPayloads.requiredLong(body, "rackDeviceId"), "add");
		RackDevicePortTypeIdentity portType = requirePortType(
				JsonPayloads.requiredLong(body, "rackDevicePortTypeId"));
		return new RackDevicePortHistory(
				identity,
				device,
				portType,
				JsonPayloads.requiredText(body, "rackDevicePortName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDevicePortHistory createUpdate(
			RackDevicePortHistory prior, JsonNode body, AssetApplyCommand command) {
		RackDeviceIdentity device = body.hasNonNull("rackDeviceId")
				? requireDevice(JsonPayloads.requiredLong(body, "rackDeviceId"), "update")
				: prior.getRackDeviceIdentity();
		RackDevicePortTypeIdentity portType = body.hasNonNull("rackDevicePortTypeId")
				? requirePortType(JsonPayloads.requiredLong(body, "rackDevicePortTypeId"))
				: prior.getRackDevicePortTypeIdentity();
		return new RackDevicePortHistory(
				prior.getRackDevicePortIdentity(),
				device,
				portType,
				JsonPayloads.requiredText(body, "rackDevicePortName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDevicePortHistory createTerminate(RackDevicePortHistory prior, AssetApplyCommand command) {
		return new RackDevicePortHistory(
				prior.getRackDevicePortIdentity(),
				prior.getRackDeviceIdentity(),
				prior.getRackDevicePortTypeIdentity(),
				prior.getRackDevicePortName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private RackDeviceIdentity requireDevice(Long rackDeviceId, String action) {
		return devices.findById(rackDeviceId)
				.orElseThrow(() -> new AssetApplyException("Rack device not found for port " + action));
	}

	private RackDevicePortTypeIdentity requirePortType(Long rackDevicePortTypeId) {
		return portTypes.findById(rackDevicePortTypeId)
				.orElseThrow(() -> new AssetApplyException(
						"Rack device port type not found: " + rackDevicePortTypeId));
	}
}
