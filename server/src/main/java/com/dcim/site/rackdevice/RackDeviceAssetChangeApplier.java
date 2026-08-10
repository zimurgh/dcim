package com.dcim.site.rackdevice;

import com.dcim.asset.AbstractAssetChangeApplier;
import com.dcim.asset.AssetApplyCommand;
import com.dcim.asset.AssetApplyException;
import com.dcim.asset.JsonPayloads;
import com.dcim.site.rack.RackIdentity;
import com.dcim.site.rack.RackIdentityRepository;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentity;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceAssetChangeApplier extends AbstractAssetChangeApplier<RackDeviceIdentity, RackDeviceHistory> {

	private final RackIdentityRepository racks;
	private final RackDeviceTypeIdentityRepository deviceTypes;

	RackDeviceAssetChangeApplier(
			RackDeviceIdentityRepository identities,
			RackDeviceHistoryRepository history,
			RackIdentityRepository racks,
			RackDeviceTypeIdentityRepository deviceTypes,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE",
				"rack device",
				identities,
				history,
				payloads,
				RackDeviceIdentity::new,
				RackDeviceIdentity::getRackDeviceId,
				RackDeviceHistory::getRackDeviceId,
				RackDeviceHistory::getRackDeviceHistoryId);
		this.racks = racks;
		this.deviceTypes = deviceTypes;
	}

	@Override
	protected RackDeviceHistory createAdd(RackDeviceIdentity identity, JsonNode body, AssetApplyCommand command) {
		RackIdentity rack = requireRack(JsonPayloads.requiredLong(body, "rackId"), "add");
		RackDeviceTypeIdentity deviceType = requireDeviceType(JsonPayloads.requiredLong(body, "rackDeviceTypeId"));
		return new RackDeviceHistory(
				identity,
				rack,
				deviceType,
				JsonPayloads.requiredText(body, "rackDeviceName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDeviceHistory createUpdate(RackDeviceHistory prior, JsonNode body, AssetApplyCommand command) {
		RackIdentity rack = body.hasNonNull("rackId")
				? requireRack(JsonPayloads.requiredLong(body, "rackId"), "update")
				: prior.getRackIdentity();
		RackDeviceTypeIdentity deviceType = body.hasNonNull("rackDeviceTypeId")
				? requireDeviceType(JsonPayloads.requiredLong(body, "rackDeviceTypeId"))
				: prior.getRackDeviceTypeIdentity();
		return new RackDeviceHistory(
				prior.getRackDeviceIdentity(),
				rack,
				deviceType,
				JsonPayloads.requiredText(body, "rackDeviceName"),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	@Override
	protected RackDeviceHistory createTerminate(RackDeviceHistory prior, AssetApplyCommand command) {
		return new RackDeviceHistory(
				prior.getRackDeviceIdentity(),
				prior.getRackIdentity(),
				prior.getRackDeviceTypeIdentity(),
				prior.getRackDeviceName(),
				command.validOn(),
				null,
				command.appliedAt(),
				command.appliedBy(),
				command.action(),
				command.committedStatus());
	}

	private RackIdentity requireRack(Long rackId, String action) {
		return racks.findById(rackId)
				.orElseThrow(() -> new AssetApplyException("Rack not found for rack device " + action));
	}

	private RackDeviceTypeIdentity requireDeviceType(Long rackDeviceTypeId) {
		return deviceTypes.findById(rackDeviceTypeId)
				.orElseThrow(() -> new AssetApplyException("Rack device type not found: " + rackDeviceTypeId));
	}
}
