package com.dcim.site.rack;

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
import com.dcim.site.cage.CageIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class RackAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("rackName", "cageId");

	private final RackHistoryRepository history;
	private final CageIdentityRepository cages;
	private final JsonPayloads payloads;

	RackAssetChangeValidator(RackHistoryRepository history, CageIdentityRepository cages, JsonPayloads payloads) {
		this.history = history;
		this.cages = cages;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "RACK".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, null, issues);
			case "UPDATE" -> {
				RackHistory prior = requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, prior, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				RackHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported rack action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, RackHistory prior, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "rackName", issues);
		String name = PayloadValidation.textOrNull(body, "rackName");

		boolean isAdd = "ADD".equals(command.action());
		Long cageId = null;
		if (isAdd || body.hasNonNull("cageId")) {
			cageId = PayloadValidation.requiredLong(body, "cageId", issues);
			if (cageId != null) {
				validateCageReference(cageId, issues);
			}
		}
		else if (prior != null) {
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

	private RackHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Rack update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<RackHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND, null, "Rack history not found: " + command.baseHistoryId()));
			return null;
		}
		RackHistory base = found.get();
		if (!base.getRackId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to rack " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale rack baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(RackHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingDevices = history.findActiveRackDeviceIdsForRack(base.getRackId()).stream()
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
}
