package com.dcim.site.rackdevicetype;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackDeviceTypeAssetChangeValidator extends AbstractAssetChangeValidator<RackDeviceTypeHistory> {

	private final RackDeviceTypeHistoryRepository history;

	RackDeviceTypeAssetChangeValidator(RackDeviceTypeHistoryRepository history, JsonPayloads payloads) {
		super(
				"RACK_DEVICE_TYPE",
				"rack device type",
				Set.of("rackDeviceTypeName", "rackDeviceTypeKind"),
				history,
				RackDeviceTypeHistory::getRackDeviceTypeId,
				payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			RackDeviceTypeHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackDeviceTypeName", issues);
		validateKind(body, issues);

		String name = PayloadValidation.textOrNull(body, "rackDeviceTypeName");
		if (name != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDeviceTypeName",
						"Another active rack device type already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			RackDeviceTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingDevices = history.findActiveRackDeviceIdsForType(prior.getRackDeviceTypeId()).stream()
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
}
