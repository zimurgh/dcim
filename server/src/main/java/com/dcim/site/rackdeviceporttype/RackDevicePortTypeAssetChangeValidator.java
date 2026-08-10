package com.dcim.site.rackdeviceporttype;

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
class RackDevicePortTypeAssetChangeValidator extends AbstractAssetChangeValidator<RackDevicePortTypeHistory> {

	private final RackDevicePortTypeHistoryRepository history;

	RackDevicePortTypeAssetChangeValidator(RackDevicePortTypeHistoryRepository history, JsonPayloads payloads) {
		super(
				"RACK_DEVICE_PORT_TYPE",
				"rack device port type",
				Set.of("rackDevicePortTypeName"),
				history,
				RackDevicePortTypeHistory::getRackDevicePortTypeId,
				payloads);
		this.history = history;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command,
			JsonNode body,
			RackDevicePortTypeHistory prior,
			List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackDevicePortTypeName", issues);
		String name = PayloadValidation.textOrNull(body, "rackDevicePortTypeName");
		if (name != null) {
			Long excludeId = prior == null ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackDevicePortTypeName",
						"Another active rack device port type already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(
			RackDevicePortTypeHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingPorts = history
				.findActiveRackDevicePortIdsForType(prior.getRackDevicePortTypeId()).stream()
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
