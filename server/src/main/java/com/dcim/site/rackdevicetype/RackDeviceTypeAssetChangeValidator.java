package com.dcim.site.rackdevicetype;

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
class RackDeviceTypeAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("rackDeviceTypeName", "rackDeviceTypeKind");

	private final RackDeviceTypeHistoryRepository history;
	private final JsonPayloads payloads;

	RackDeviceTypeAssetChangeValidator(RackDeviceTypeHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK_DEVICE_TYPE".equals(assetType);
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
				RackDeviceTypeHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION,
					null,
					"Unsupported rack device type action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "rackDeviceTypeName", issues);
		validateKind(body, issues);

		String name = PayloadValidation.textOrNull(body, "rackDeviceTypeName");
		if (name != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDeviceTypeName",
						"Another active rack device type already uses name: " + name));
			}
		}
	}

	private static void validateKind(JsonNode body, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackDeviceTypeKind", issues);
		String raw = PayloadValidation.textOrNull(body, "rackDeviceTypeKind");
		if (raw == null) {
			return;
		}
		try {
			RackDeviceTypeKind.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE,
					"rackDeviceTypeKind",
					"rackDeviceTypeKind must be Patch Panel, Extranet Switch, Matrix Switch, or Tap: " + raw));
		}
	}

	private RackDeviceTypeHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Rack device type update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<RackDeviceTypeHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Rack device type history not found: " + command.baseHistoryId()));
			return null;
		}
		RackDeviceTypeHistory base = found.get();
		if (!base.getRackDeviceTypeId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to rack device type " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale rack device type baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(
			RackDeviceTypeHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingDevices = history.findActiveRackDeviceIdsForType(base.getRackDeviceTypeId()).stream()
				.filter(id -> !context.coversTerminate("RACK_DEVICE", id))
				.toList();
		if (blockingDevices.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Rack device type is referenced by active rack devices",
				blockingDevices.toArray(new Long[0])));
	}
}
