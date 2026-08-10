package com.dcim.site.cage;

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
import com.dcim.site.datacenter.DataCenterIdentityRepository;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
class CageAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("cageName", "dataCenterId");

	private final CageHistoryRepository history;
	private final DataCenterIdentityRepository dataCenters;
	private final JsonPayloads payloads;

	CageAssetChangeValidator(
			CageHistoryRepository history, DataCenterIdentityRepository dataCenters, JsonPayloads payloads) {
		this.history = history;
		this.dataCenters = dataCenters;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "CAGE".equals(assetType);
	}

	@Override
	public List<ValidationIssue> validate(AssetValidateCommand command, ValidationContext context) {
		List<ValidationIssue> issues = new ArrayList<>();
		JsonNode body = payloads.read(command.payloadJson());
		switch (command.action()) {
			case "ADD" -> validateAddOrUpdate(command, body, null, issues);
			case "UPDATE" -> {
				CageHistory prior = requireCurrentBase(command, issues);
				validateAddOrUpdate(command, body, prior, issues);
			}
			case "TERMINATE" -> {
				issues.addAll(PayloadValidation.unknownFields(body, Set.of()));
				CageHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported cage action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(
			AssetValidateCommand command, JsonNode body, CageHistory prior, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "cageName", issues);
		String name = PayloadValidation.textOrNull(body, "cageName");

		boolean isAdd = "ADD".equals(command.action());
		Long dataCenterId = null;
		if (isAdd || body.hasNonNull("dataCenterId")) {
			dataCenterId = PayloadValidation.requiredLong(body, "dataCenterId", issues);
			if (dataCenterId != null) {
				validateDataCenterReference(dataCenterId, issues);
			}
		}
		else if (prior != null) {
			dataCenterId = prior.getDataCenterId();
		}

		if (name != null && dataCenterId != null) {
			Long excludeId = isAdd ? null : command.assetIdentityId();
			if (history.existsActiveNameClashInDataCenter(name, dataCenterId, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"cageName",
						"Another active cage in this data center already uses name: " + name));
			}
		}
	}

	private void validateDataCenterReference(Long dataCenterId, List<ValidationIssue> issues) {
		if (!dataCenters.existsById(dataCenterId)) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_FOUND,
					"dataCenterId",
					"Data center not found: " + dataCenterId));
		}
		else if (history.countActiveDataCenters(dataCenterId) == 0) {
			issues.add(ValidationIssue.of(
					ValidationCodes.REFERENCE_NOT_ACTIVE,
					"dataCenterId",
					"Data center is not currently active: " + dataCenterId));
		}
	}

	private CageHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Cage update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<CageHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND, null, "Cage history not found: " + command.baseHistoryId()));
			return null;
		}
		CageHistory base = found.get();
		if (!base.getCageId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to cage " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale cage baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(CageHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingRacks = history.findActiveRackIdsForCage(base.getCageId()).stream()
				.filter(id -> !context.coversTerminate("RACK", id))
				.toList();
		if (blockingRacks.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN, null, "Cage has active racks", blockingRacks.toArray(new Long[0])));
	}
}
