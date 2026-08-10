package com.dcim.site.rackdeviceporttype;

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

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDevicePortTypeAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("rackDevicePortTypeName");

	private final RackDevicePortTypeHistoryRepository history;
	private final JsonPayloads payloads;

	RackDevicePortTypeAssetChangeValidator(RackDevicePortTypeHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_PORT_TYPE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, issues);
			case "UPDATE" -> {
				requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				RackDevicePortTypeHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION,
					null,
					"Unsupported rack device port type action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "rackDevicePortTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDevicePortTypeName");
		if (name != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDevicePortTypeName",
						"Another active rack device port type already uses name: " + name));
			}
		}
	}

	private RackDevicePortTypeHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Rack device port type update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<RackDevicePortTypeHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Rack device port type history not found: " + command.baseHistoryId()));
			return null;
		}
		RackDevicePortTypeHistory base = found.get();
		if (!base.getRackDevicePortTypeId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to rack device port type " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale rack device port type baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(
			RackDevicePortTypeHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingPorts = history
				.findActiveRackDevicePortIdsForType(base.getRackDevicePortTypeId()).stream()
				.filter(id -> !context.coversTerminate("RACK_DEVICE_PORT", id))
				.toList();
		if (blockingPorts.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Rack device port type is referenced by active rack device ports",
				blockingPorts.toArray(new Long[0])));
	}
}
