package com.dcim.organization.firm;

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
class FirmAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("firmName", "parentFirmName");

	private final FirmHistoryRepository history;
	private final JsonPayloads payloads;

	FirmAssetChangeValidator(FirmHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "FIRM".equals(assetType);
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
				FirmHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateReferences(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION, null, "Unsupported firm action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "firmName", issues);
		String firmName = PayloadValidation.textOrNull(body, "firmName");
		if (firmName != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(firmName, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"firmName",
						"Another active firm already uses name: " + firmName));
			}
		}
	}

	private FirmHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Firm update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<FirmHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND, null, "Firm history not found: " + command.baseHistoryId()));
			return null;
		}
		FirmHistory base = found.get();
		if (!base.getFirmId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to firm " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale firm baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateReferences(FirmHistory base, ValidationContext context, List<ValidationIssue> issues) {
		Long firmId = base.getFirmId();
		List<Long> blockingCrossConnects = history.findActiveCrossConnectIdsReferencingFirm(firmId).stream()
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		List<Long> blockingFeeds = history.findActiveMarketDataFeedIdsReferencingFirm(firmId).stream()
				.filter(id -> !context.coversTerminate("MARKET_DATA_FEED", id))
				.toList();
		if (blockingCrossConnects.isEmpty() && blockingFeeds.isEmpty()) {
			return;
		}
		List<Long> related = new ArrayList<>();
		related.addAll(blockingCrossConnects);
		related.addAll(blockingFeeds);
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_REFERENCES,
				null,
				"Firm is referenced by active cross connects or market data feeds",
				related.toArray(new Long[0])));
	}
}
