package com.dcim.site.rackdeviceport;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.dcim.asset.AssetChangeValidator;
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
class RackDevicePortAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS =
			Set.of("rackDevicePortName", "rackDeviceId", "rackDevicePortTypeId");

	private final RackDevicePortHistoryRepository history;
	private final RackDeviceIdentityRepository devices;
	private final RackDevicePortTypeIdentityRepository portTypes;
	private final JsonPayloads payloads;

	RackDevicePortAssetChangeValidator(
			RackDevicePortHistoryRepository history,
			RackDeviceIdentityRepository devices,
			RackDevicePortTypeIdentityRepository portTypes,
			JsonPayloads payloads) {
		this.history = history;
		this.devices = devices;
		this.portTypes = portTypes;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_PORT".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, null, issues);
			case "UPDATE" -> {
				RackDevicePortHistory prior = requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, prior, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				RackDevicePortHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION,
					null,
					"Unsupported rack device port action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackDevicePortHistory prior, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "rackDevicePortName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDevicePortName");

		boolean isAdd = "ADD".equals(command.action());
		Long rackDeviceId = null;
		if (isAdd || body.hasNonNull("rackDeviceId")) {
			rackDeviceId = PayloadValidation.requiredLong(body, "rackDeviceId", issues);
			if (rackDeviceId != null) {
				validateRackDeviceReference(rackDeviceId, issues);
			}
		}
		else if (prior != null) {
			rackDeviceId = prior.getRackDeviceId();
		}

		Long rackDevicePortTypeId = null;
		if (isAdd || body.hasNonNull("rackDevicePortTypeId")) {
			rackDevicePortTypeId = PayloadValidation.requiredLong(body, "rackDevicePortTypeId", issues);
			if (rackDevicePortTypeId != null) {
				validateRackDevicePortTypeReference(rackDevicePortTypeId, issues);
			}
		}
		else if (prior != null) {
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

	private RackDevicePortHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Rack device port update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<RackDevicePortHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Rack device port history not found: " + command.baseHistoryId()));
			return null;
		}
		RackDevicePortHistory base = found.get();
		if (!base.getRackDevicePortId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to rack device port " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale rack device port baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(
			RackDevicePortHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingCables = history.findActiveCableIdsForPort(base.getRackDevicePortId()).stream()
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
}
