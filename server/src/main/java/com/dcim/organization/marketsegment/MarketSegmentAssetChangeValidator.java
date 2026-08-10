package com.dcim.organization.marketsegment;

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
class MarketSegmentAssetChangeValidator implements AssetChangeValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("marketSegmentName", "marketSegmentType");

	private final MarketSegmentHistoryRepository history;
	private final JsonPayloads payloads;

	MarketSegmentAssetChangeValidator(MarketSegmentHistoryRepository history, JsonPayloads payloads) {
		this.history = history;
		this.payloads = payloads;
	}

	@Override
	public boolean supports(String assetType) {
		return "MARKET_SEGMENT".equals(assetType);
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
				MarketSegmentHistory base = requireCurrentBase(command, issues);
				if (base != null) {
					validateTerminateReferences(base, context, issues);
				}
			}
			default -> issues.add(ValidationIssue.of(
					ValidationCodes.UNSUPPORTED_ACTION,
					null,
					"Unsupported market segment action: " + command.action()));
		}
		return issues;
	}

	private void validateAddOrUpdate(AssetValidateCommand command, JsonNode body, List<ValidationIssue> issues) {
		issues.addAll(PayloadValidation.unknownFields(body, ALLOWED_FIELDS));
		PayloadValidation.requireText(body, "marketSegmentName", issues);
		validateMarketSegmentType(body, issues);

		String name = PayloadValidation.textOrNull(body, "marketSegmentName");
		if (name != null) {
			Long excludeId = "ADD".equals(command.action()) ? null : command.assetIdentityId();
			if (history.existsActiveNameClash(name, excludeId)) {
				issues.add(ValidationIssue.of(
						ValidationCodes.NAME_CLASH,
						"marketSegmentName",
						"Another active market segment already uses name: " + name));
			}
		}
	}

	private static void validateMarketSegmentType(JsonNode body, List<ValidationIssue> issues) {
		PayloadValidation.requireText(body, "marketSegmentType", issues);
		String raw = PayloadValidation.textOrNull(body, "marketSegmentType");
		if (raw == null) {
			return;
		}
		try {
			MarketSegmentType.fromPayload(raw);
		}
		catch (IllegalArgumentException ex) {
			issues.add(ValidationIssue.of(
					ValidationCodes.INVALID_VALUE,
					"marketSegmentType",
					"marketSegmentType must be Equities Index or Agricultural Futures: " + raw));
		}
	}

	private MarketSegmentHistory requireCurrentBase(AssetValidateCommand command, List<ValidationIssue> issues) {
		if (command.assetIdentityId() == null || command.baseHistoryId() == null) {
			issues.add(ValidationIssue.of(
					ValidationCodes.MISSING_IDENTITY,
					null,
					"Market segment update/terminate requires assetIdentityId and baseHistoryId"));
			return null;
		}
		Optional<MarketSegmentHistory> found = history.findById(command.baseHistoryId());
		if (found.isEmpty()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.HISTORY_NOT_FOUND,
					null,
					"Market segment history not found: " + command.baseHistoryId()));
			return null;
		}
		MarketSegmentHistory base = found.get();
		if (!base.getMarketSegmentId().equals(command.assetIdentityId())) {
			issues.add(ValidationIssue.of(
					ValidationCodes.IDENTITY_MISMATCH,
					null,
					"baseHistoryId does not belong to market segment " + command.assetIdentityId()));
			return null;
		}
		if (!base.isCurrent()) {
			issues.add(ValidationIssue.of(
					ValidationCodes.STALE_BASE,
					null,
					"Stale market segment baseHistoryId (already closed): " + command.baseHistoryId()));
			return null;
		}
		return base;
	}

	private void validateTerminateReferences(
			MarketSegmentHistory base, ValidationContext context, List<ValidationIssue> issues) {
		Long marketSegmentId = base.getMarketSegmentId();
		List<Long> blockingCrossConnects = history
				.findActiveCrossConnectIdsReferencingMarketSegment(marketSegmentId).stream()
				.filter(id -> !context.coversTerminate("CROSS_CONNECT", id))
				.toList();
		if (blockingCrossConnects.isEmpty()) {
			return;
		}
		issues.add(ValidationIssue.of(
				ValidationCodes.ACTIVE_REFERENCES,
				null,
				"Market segment is referenced by active cross connects",
				blockingCrossConnects.toArray(new Long[0])));
	}
}
