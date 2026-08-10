package com.dcim.site.rack;

import java.util.List;
import java.util.Set;

import com.dcim.asset.AbstractAssetChangeValidator;
import com.dcim.asset.AssetValidateCommand;
import com.dcim.asset.JsonPayloads;
import com.dcim.asset.PayloadValidation;
import com.dcim.asset.ValidationCodes;
import com.dcim.asset.ValidationContext;
import com.dcim.asset.ValidationIssue;
import com.dcim.site.cage.CageIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackAssetChangeValidator extends AbstractAssetChangeValidator<RackHistory> {

	private final RackHistoryRepository history;
	private final CageIdentityRepository cages;

	RackAssetChangeValidator(RackHistoryRepository history, CageIdentityRepository cages, JsonPayloads payloads) {
		super("RACK", "rack", Set.of("rackName", "cageId"), history, RackHistory::getRackId, payloads);
		this.history = history;
		this.cages = cages;
	}

	@Override
	protected void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackHistory prior, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "rackName", issues);
		String name = PayloadValidation.textOrNull(body, "rackName");

		boolean isAdd = prior == null;
		Long cageId = null;
		if (isAdd || body.hasNonNull("cageId")) {
			cageId = PayloadValidation.requiredLong(body, "cageId", issues);
			if (cageId != null) {
				validateCageReference(cageId, issues);
			}
		}
		else {
			cageId = prior.getCageId();
		}

		if (name != null && cageId != null) {
			Long excludeId = isAdd ? null : command.assetIdentityId();
			if (history.existsActiveNameClashInCage(name, cageId, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"rackName",
						"Another active rack in this cage already uses name: " + name));
			}
		}
	}

	@Override
	protected void validateTerminate(RackHistory prior, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingDevices = history.findActiveRackDeviceIdsForRack(prior.getRackId()).stream()
				.filter(id -> !context.coversTerminate("RACK_DEVICE", id))
				.toList();
		if (blockingDevices.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Rack has active rack devices",
				blockingDevices.toArray(new Long[0])));
	}

	private void validateCageReference(Long cageId, List<ValidationIssue> issues) {
		if (!cages.existsById(cageId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND, "cageId", "Cage not found: " + cageId));
		}
		else if (history.countActiveCages(cageId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE, "cageId", "Cage is not currently active: " + cageId));
		}
	}
}
