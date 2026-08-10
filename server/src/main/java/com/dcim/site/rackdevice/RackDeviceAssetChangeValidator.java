package com.dcim.site.rackdevice;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.site.rack.RackIdentityRepository;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceAssetChangeValidator extends AbstractAssetChangeValidator<RackDeviceHistory> {

	private final RackDeviceHistoryRepository history;
	private final RackIdentityRepository racks;
	private final RackDeviceTypeIdentityRepository deviceTypes;

	RackDeviceAssetChangeValidator(
			RackDeviceHistoryRepository history,
			RackIdentityRepository racks,
			RackDeviceTypeIdentityRepository deviceTypes,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE",
				"rack device",
				Set.of("rackDeviceName", "rackId", "rackDeviceTypeId"),
				history,
				RackDeviceHistory::getRackDeviceId,
				payloads);
		this.history = history;
		this.racks = racks;
		this.deviceTypes = deviceTypes;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackDeviceHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackDeviceName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDeviceName");

		boolean isAdd = prior == null;
		Long rackId = null;
		if (isAdd || body.hasNonNull("rackId")) {
			rackId = PayloadValidation.requiredLong(body, "rackId", issues);
			if (rackId != null) {
				validateRackReference(rackId, issues);
			}
		}
		else {
			rackId = prior.getRackId();
		}

		Long rackDeviceTypeId = null;
		if (isAdd || body.hasNonNull("rackDeviceTypeId")) {
			rackDeviceTypeId = PayloadValidation.requiredLong(body, "rackDeviceTypeId", issues);
			if (rackDeviceTypeId != null) {
				validateRackDeviceTypeReference(rackDeviceTypeId, issues);
			}
		}
		else {
			rackDeviceTypeId = prior.getRackDeviceTypeId();
		}

		if (name != null && rackId != null) {
			Long excludeId = isAdd ? null : command.assetIdentityId();
			if (history.existsActiveNameClashInRack(name, rackId, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDeviceName",
						"Another active rack device in this rack already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			RackDeviceHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingPorts = history.findActiveRackDevicePortIdsForDevice(prior.getRackDeviceId()).stream()
				.filter(id -> !context.coversTerminate("RACK_DEVICE_PORT", id))
				.toList();
		if (blockingPorts.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Rack device has active ports",
				blockingPorts.toArray(new Long[0])));
	}

	private void validateRackReference(Long rackId, List<ValidationIssue> issues) {
		if (!racks.existsById(rackId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND, "rackId", "Rack not found: " + rackId));
		}
		else if (history.countActiveRacks(rackId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE, "rackId", "Rack is not currently active: " + rackId));
		}
	}

	private void validateRackDeviceTypeReference(Long rackDeviceTypeId, List<ValidationIssue> issues) {
		if (!deviceTypes.existsById(rackDeviceTypeId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND,
					"rackDeviceTypeId",
					"Rack device type not found: " + rackDeviceTypeId));
		}
		else if (history.countActiveRackDeviceTypes(rackDeviceTypeId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE,
					"rackDeviceTypeId",
					"Rack device type is not currently active: " + rackDeviceTypeId));
		}
	}
}
