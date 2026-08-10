package com.dcim.site.rackdevice;

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
import com.dcim.site.rack.RackIdentityRepository;
import com.dcim.site.rackdevicetype.RackDeviceTypeIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("rackDeviceName", "rackId", "rackDeviceTypeId");

	private final RackDeviceHistoryRepository history;
	private final RackIdentityRepository racks;
	private final RackDeviceTypeIdentityRepository deviceTypes;
	private final JsonPayloads payloads;

	RackDeviceAssetChangeValidator(
			RackDeviceHistoryRepository history,
			RackIdentityRepository racks,
			RackDeviceTypeIdentityRepository deviceTypes,
			JsonPayloads payloads) {
		this.history = history;
		this.racks = racks;
		this.deviceTypes = deviceTypes;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, null, issues);
			case "UPDATE" -> {
				RackDeviceHistory prior = requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, prior, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				RackDeviceHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported rack device action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackDeviceHistory prior, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "rackDeviceName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDeviceName");

		boolean isAdd = "ADD".equals(command.action());
		Long rackId = null;
		if (isAdd || body.hasNonNull("rackId")) {
			rackId = PayloadValidation.requiredLong(body, "rackId", issues);
			if (rackId != null) {
				validateRackReference(rackId, issues);
			}
		}
		else if (prior != null) {
			rackId = prior.getRackId();
		}

		Long rackDeviceTypeId = null;
		if (isAdd || body.hasNonNull("rackDeviceTypeId")) {
			rackDeviceTypeId = PayloadValidation.requiredLong(body, "rackDeviceTypeId", issues);
			if (rackDeviceTypeId != null) {
				validateRackDeviceTypeReference(rackDeviceTypeId, issues);
			}
		}
		else if (prior != null) {
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

	private RackDeviceHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Rack device update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<RackDeviceHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Rack device history not found: " + command.baseHistoryId()));
			return null;
		}
		RackDeviceHistory base = found.get();
		if (!base.getRackDeviceId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to rack device " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale rack device baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(
			RackDeviceHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingPorts = history.findActiveRackDevicePortIdsForDevice(base.getRackDeviceId()).stream()
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
}
