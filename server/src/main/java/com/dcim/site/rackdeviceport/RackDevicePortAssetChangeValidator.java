package com.dcim.site.rackdeviceport;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.site.rackdevice.RackDeviceIdentityRepository;
import com.dcim.site.rackdeviceporttype.RackDevicePortTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDevicePortAssetChangeValidator extends AbstractAssetChangeValidator<RackDevicePortHistory> {

	private final RackDevicePortHistoryRepository history;
	private final RackDeviceIdentityRepository devices;
	private final RackDevicePortTypeIdentityRepository portTypes;

	RackDevicePortAssetChangeValidator(
			RackDevicePortHistoryRepository history,
			RackDeviceIdentityRepository devices,
			RackDevicePortTypeIdentityRepository portTypes,
			JsonPayloads payloads) {
		super(
				"RACK_DEVICE_PORT",
				"rack device port",
				Set.of("rackDevicePortName", "rackDeviceId", "rackDevicePortTypeId"),
				history,
				RackDevicePortHistory::getRackDevicePortId,
				payloads);
		this.history = history;
		this.devices = devices;
		this.portTypes = portTypes;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackDevicePortHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackDevicePortName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDevicePortName");

		boolean isAdd = prior == null;
		Long rackDeviceId = null;
		if (isAdd || body.hasNonNull("rackDeviceId")) {
			rackDeviceId = PayloadValidation.requiredLong(body, "rackDeviceId", issues);
			if (rackDeviceId != null) {
				validateRackDeviceReference(rackDeviceId, issues);
			}
		}
		else {
			rackDeviceId = prior.getRackDeviceId();
		}

		Long rackDevicePortTypeId = null;
		if (isAdd || body.hasNonNull("rackDevicePortTypeId")) {
			rackDevicePortTypeId = PayloadValidation.requiredLong(body, "rackDevicePortTypeId", issues);
			if (rackDevicePortTypeId != null) {
				validateRackDevicePortTypeReference(rackDevicePortTypeId, issues);
			}
		}
		else {
			rackDevicePortTypeId = prior.getRackDevicePortTypeId();
		}

		if (name != null && rackDeviceId != null) {
			Long excludeId = isAdd ? null : command.assetIdentityId();
			if (history.existsActiveNameClashInDevice(name, rackDeviceId, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDevicePortName",
						"Another active port on this rack device already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			RackDevicePortHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingCables = history.findActiveCableIdsForPort(prior.getRackDevicePortId()).stream()
				.filter(id -> !context.coversTerminate("CABLE", id))
				.toList();
		if (blockingCables.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Rack device port has active cables",
				blockingCables.toArray(new Long[0])));
	}

	private void validateRackDeviceReference(Long rackDeviceId, List<ValidationIssue> issues) {
		if (!devices.existsById(rackDeviceId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND, "rackDeviceId", "Rack device not found: " + rackDeviceId));
		}
		else if (history.countActiveRackDevices(rackDeviceId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE,
					"rackDeviceId",
					"Rack device is not currently active: " + rackDeviceId));
		}
	}

	private void validateRackDevicePortTypeReference(Long rackDevicePortTypeId, List<ValidationIssue> issues) {
		if (!portTypes.existsById(rackDevicePortTypeId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND,
					"rackDevicePortTypeId",
					"Rack device port type not found: " + rackDevicePortTypeId));
		}
		else if (history.countActiveRackDevicePortTypes(rackDevicePortTypeId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE,
					"rackDevicePortTypeId",
					"Rack device port type is not currently active: " + rackDevicePortTypeId));
		}
	}
}
