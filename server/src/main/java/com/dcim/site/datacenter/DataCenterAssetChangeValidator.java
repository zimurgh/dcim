package com.dcim.site.datacenter;

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
class DataCenterAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("dataCenterName");

	private final DataCenterHistoryRepository history;
	private final JsonPayloads payloads;

	DataCenterAssetChangeValidator(DataCenterHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "DATA_CENTER".equals(assetType);
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
				DataCenterHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateChildren(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported data center action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "dataCenterName", issues);
		String name = PayloadValidation.textOrNull(body, "dataCenterName");
		if (name != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"dataCenterName",
						"Another active data center already uses name: " + name));
			}
		}
	}

	private DataCenterHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Data center update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<DataCenterHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Data center history not found: " + command.baseHistoryId()));
			return null;
		}
		DataCenterHistory base = found.get();
		if (!base.getDataCenterId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to data center " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale data center baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateChildren(
			DataCenterHistory base, ValidationContext context, List<ValidationIssue> issues) {
		List<Long> blockingCages = history.findActiveCageIdsForDataCenter(base.getDataCenterId()).stream()
				.filter(id -> !context.coversTerminate("CAGE", id))
				.toList();
		if (blockingCages.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_CHILDREN,
				null,
				"Data center has active cages",
				blockingCages.toArray(new Long[0])));
	}
}
